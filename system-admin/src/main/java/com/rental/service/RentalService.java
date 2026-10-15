package com.rental.service;

import com.rental.common.*;
import com.rental.domain.Models.*;
import com.rental.persistence.RentalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import org.slf4j.MDC;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** 签约、账单、收款和退租以数据库事务维护业务不变量。 */
@Service
public class RentalService {
    @Resource private RentalMapper mapper;
    public Map<String,Object> query(String query,String status,int page,int size) {
        int offset=Page.offset(page,size);
        if (query!=null && query.length()>100) throw BusinessException.bad("查询词最多 100 字符");
        Map<String,Object> q=new HashMap<>(); q.put("query",query);q.put("status",status);q.put("offset",offset);q.put("size",size);q.put("contractId",null);
        return q;
    }
    public Page<House> houses(String q,String status,int page,int size) { Map<String,Object> query=query(q,status,page,size); return new Page<>(mapper.houses(query),mapper.houseCount(query),page,size); }
    public Page<Tenant> tenants(String q,int page,int size) { Map<String,Object> query=query(q,null,page,size);return new Page<>(mapper.tenants(query),mapper.tenantCount(query),page,size); }
    public Page<Contract> contracts(String q,String status,int page,int size) {Map<String,Object> query=query(q,status,page,size);return new Page<>(mapper.contracts(query),mapper.contractCount(query),page,size);}
    public Page<Bill> bills(String q,String status,Long contractId,int page,int size) {Map<String,Object> query=query(q,status,page,size);query.put("contractId",contractId);return new Page<>(mapper.bills(query),mapper.billCount(query),page,size);}

    @Transactional public House saveHouse(Long id,HouseInput input,String actor) {
        Map<String,Object> values=values(input); values.put("id",id);
        if (id==null) {mapper.addHouse(values);id=((Number)values.get("id")).longValue();}
        else {
            House current=mapper.lockHouse(id);if(current==null)throw BusinessException.missing("房源不存在");
            // 已出租状态只能由合同驱动，编辑房源不能绕过合同释放占用。
            if ("OCCUPIED".equals(current.getStatus())) values.put("status","OCCUPIED");
            if(mapper.editHouse(values)!=1)throw BusinessException.conflict("房源已被其他操作更新，请刷新后重试");
        }
        audit(actor,"HOUSE_SAVE",String.valueOf(id));return mapper.house(id);
    }
    @Transactional public void deleteHouse(Long id,String actor) {
        House house=mapper.lockHouse(id);if(house==null)throw BusinessException.missing("房源不存在");
        if("OCCUPIED".equals(house.getStatus()))throw BusinessException.conflict("已出租房源不能删除");
        mapper.deleteHouse(id);audit(actor,"HOUSE_DELETE",String.valueOf(id));
    }
    @Transactional public Tenant saveTenant(Long id,TenantInput input,String actor) {
        Map<String,Object> values=values(input);values.put("id",id);
        if(id==null){mapper.addTenant(values);id=((Number)values.get("id")).longValue();}
        else {if(mapper.lockTenant(id)==null)throw BusinessException.missing("租客不存在");mapper.editTenant(values);}
        audit(actor,"TENANT_SAVE",String.valueOf(id));return mapper.lockTenant(id);
    }
    @Transactional public void deleteTenant(Long id,String actor) {
        if(mapper.lockTenant(id)==null)throw BusinessException.missing("租客不存在");
        if(mapper.activeTenant(id)>0)throw BusinessException.conflict("租客仍有生效合同，不能删除");
        mapper.deleteTenant(id);audit(actor,"TENANT_DELETE",String.valueOf(id));
    }
    @Transactional public Contract sign(ContractInput input,String actor) {
        LocalDate start=RentalRules.date(input.getStartDate()); LocalDate end=RentalRules.endDate(start,input.getMonths());
        // 固定先锁房源再锁租客；并发签约只能有一个请求看到 VACANT。
        House house=mapper.lockHouse(input.getHouseId());
        if(house==null)throw BusinessException.missing("房源不存在");
        if(!"VACANT".equals(house.getStatus()))throw BusinessException.conflict("房源不处于可出租状态");
        if(mapper.lockTenant(input.getTenantId())==null)throw BusinessException.missing("租客不存在");
        Contract contract=new Contract();BeanUtils.copyProperties(input,contract);
        contract.setContractNo("CT-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());
        contract.setMonthlyRent(house.getMonthlyRent());contract.setDeposit(house.getDeposit());
        contract.setStartDate(start.toString());contract.setEndDate(end.toString());mapper.addContract(contract);
        for(int index=0;index<input.getMonths();index++) {
            LocalDate due=start.plusMonths(index);Bill bill=new Bill();bill.setContractId(contract.getId());
            bill.setPeriod(YearMonth.from(due).toString());bill.setDueDate(due.toString());bill.setAmount(house.getMonthlyRent());mapper.addBill(bill);
        }
        mapper.houseStatus(house.getId(),"OCCUPIED");audit(actor,"CONTRACT_SIGN",contract.getContractNo());return mapper.contract(contract.getId());
    }
    @Transactional public Contract close(Long id,String actor) {
        Contract reference=mapper.contract(id);if(reference==null)throw BusinessException.missing("合同不存在");
        // 与签约保持房源在先的锁顺序，账单按 ID 加锁；收款先锁合同再锁账单。
        mapper.lockHouse(reference.getHouseId());Contract contract=mapper.lockContract(id);
        if(!"ACTIVE".equals(contract.getStatus()))throw BusinessException.conflict("合同已结束");
        for(Bill bill:mapper.lockContractBills(id)) {
            if("CANCELLED".equals(bill.getStatus()))continue;
            boolean future=RentalRules.date(bill.getDueDate()).isAfter(LocalDate.now());
            if(!future && bill.getPaidAmount().compareTo(bill.getAmount())<0)throw BusinessException.conflict("请先结清已到期账单");
            if(future && bill.getPaidAmount().signum()>0)throw BusinessException.conflict("未来账单已有收款，需先处理退款后再退租");
        }
        mapper.cancelFuture(id);mapper.closeContract(id);mapper.houseStatus(contract.getHouseId(),"VACANT");
        audit(actor,"CONTRACT_CLOSE",contract.getContractNo());return mapper.contract(id);
    }
    // READ_COMMITTED 让等待行锁后的重复请求可见前一笔提交的幂等记录。
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Payment pay(Long billId,PaymentInput input,String actor) {
        Bill reference=mapper.bill(billId);if(reference==null)throw BusinessException.missing("账单不存在");
        mapper.lockContract(reference.getContractId());Bill bill=mapper.lockBill(billId);
        // 相同账单的并发请求由行锁串行化；唯一索引覆盖跨账单的幂等号竞争。
        Payment existing=mapper.paymentByRequest(input.getRequestId());
        if(existing!=null) {
            if(!existing.getBillId().equals(billId) || existing.getAmount().compareTo(input.getAmount())!=0
                    || !existing.getMethod().equals(input.getMethod()) || !Objects.equals(normalize(existing.getNote()),normalize(input.getNote())))
                throw BusinessException.conflict("幂等号已被不同的收款请求使用");
            return existing;
        }
        if("CANCELLED".equals(bill.getStatus()))throw BusinessException.conflict("已取消账单不能收款");
        BigDecimal paid=RentalRules.received(bill.getAmount(),bill.getPaidAmount(),input.getAmount());
        Payment payment=new Payment();BeanUtils.copyProperties(input,payment);payment.setBillId(billId);payment.setActor(actor);
        mapper.addPayment(payment);mapper.billPaid(billId,paid,paid.compareTo(bill.getAmount())==0?"PAID":"PARTIAL");
        audit(actor,"PAYMENT_RECEIVE",String.valueOf(payment.getId()));return mapper.paymentByRequest(input.getRequestId());
    }
    public List<Payment> payments(Long id){if(mapper.bill(id)==null)throw BusinessException.missing("账单不存在");return mapper.payments(id);}
    public Page<Audit> audits(int page,int size){return new Page<>(mapper.audits(Page.offset(page,size),size),mapper.auditCount(),page,size);}
    public Map<String,Object> dashboard() {
        Map<String,Object> result=new LinkedHashMap<>();result.putAll(mapper.houseStats());result.putAll(mapper.billStats());
        result.put("monthIncome",mapper.monthIncome());result.put("activeContracts",mapper.activeContracts());result.put("trend",mapper.incomeTrend());return result;
    }
    public void audit(String actor,String action,String target) {
        Audit audit=new Audit();audit.setActor(actor);audit.setAction(action);audit.setTarget(target);
        audit.setRequestId(Optional.ofNullable(MDC.get("requestId")).orElse("bootstrap"));mapper.audit(audit);
    }
    private String normalize(String value){return value==null?"":value;}
    private Map<String,Object> values(Object bean) {
        Map<String,Object> result=new HashMap<>();
        org.springframework.beans.BeanWrapper wrapper=new org.springframework.beans.BeanWrapperImpl(bean);
        for(java.beans.PropertyDescriptor property:wrapper.getPropertyDescriptors()) if(!"class".equals(property.getName())) result.put(property.getName(),wrapper.getPropertyValue(property.getName()));
        return result;
    }
}
