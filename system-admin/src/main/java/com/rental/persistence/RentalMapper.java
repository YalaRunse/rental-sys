package com.rental.persistence;

import com.rental.domain.Models.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/** 租赁业务 SQL。所有输入参数绑定，列表排序固定；写锁必须在服务事务中获取。 */
public interface RentalMapper {
    String HOUSE_FILTER = " WHERE deleted=0 <if test='query != null and query != &quot;&quot;'>AND (code LIKE CONCAT('%',#{query},'%') OR title LIKE CONCAT('%',#{query},'%') OR address LIKE CONCAT('%',#{query},'%'))</if><if test='status != null and status != &quot;&quot;'>AND status=#{status}</if>";
    String TENANT_FILTER = " WHERE deleted=0 <if test='query != null and query != &quot;&quot;'>AND (name LIKE CONCAT('%',#{query},'%') OR phone LIKE CONCAT('%',#{query},'%'))</if>";
    String CONTRACT_FROM = " FROM rental_contract c JOIN rental_house h ON c.house_id=h.id JOIN rental_tenant t ON c.tenant_id=t.id ";
    String CONTRACT_FILTER = " WHERE 1=1 <if test='query != null and query != &quot;&quot;'>AND (c.contract_no LIKE CONCAT('%',#{query},'%') OR h.title LIKE CONCAT('%',#{query},'%') OR t.name LIKE CONCAT('%',#{query},'%'))</if><if test='status != null and status != &quot;&quot;'>AND c.status=#{status}</if>";
    String BILL_FROM = " FROM rental_bill b JOIN rental_contract c ON b.contract_id=c.id JOIN rental_house h ON c.house_id=h.id JOIN rental_tenant t ON c.tenant_id=t.id ";
    String BILL_FILTER = " WHERE 1=1 <if test='query != null and query != &quot;&quot;'>AND (c.contract_no LIKE CONCAT('%',#{query},'%') OR t.name LIKE CONCAT('%',#{query},'%') OR h.title LIKE CONCAT('%',#{query},'%'))</if><if test='status != null and status != &quot;&quot;'>AND b.status=#{status}</if><if test='contractId != null'>AND c.id=#{contractId}</if>";

    @Select("<script>SELECT * FROM rental_house"+HOUSE_FILTER+" ORDER BY id DESC LIMIT #{size} OFFSET #{offset}</script>") List<House> houses(Map<String,Object> q);
    @Select("<script>SELECT COUNT(*) FROM rental_house"+HOUSE_FILTER+"</script>") long houseCount(Map<String,Object> q);
    @Select("SELECT * FROM rental_house WHERE id=#{id} AND deleted=0 FOR UPDATE") House lockHouse(Long id);
    @Select("SELECT * FROM rental_house WHERE id=#{id} AND deleted=0") House house(Long id);
    @Insert("INSERT INTO rental_house(code,title,address,area,monthly_rent,deposit,status) VALUES(#{code},#{title},#{address},#{area},#{monthlyRent},#{deposit},#{status})")
    @Options(useGeneratedKeys=true,keyProperty="id") void addHouse(Map<String,Object> h);
    @Update("UPDATE rental_house SET code=#{code},title=#{title},address=#{address},area=#{area},monthly_rent=#{monthlyRent},deposit=#{deposit},status=#{status},version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0") int editHouse(Map<String,Object> h);
    @Update("UPDATE rental_house SET status=#{status},version=version+1 WHERE id=#{id}") void houseStatus(@Param("id") Long id,@Param("status") String status);
    @Update("UPDATE rental_house SET deleted=1,version=version+1 WHERE id=#{id}") void deleteHouse(Long id);
    @Select("<script>SELECT * FROM rental_tenant"+TENANT_FILTER+" ORDER BY id DESC LIMIT #{size} OFFSET #{offset}</script>") List<Tenant> tenants(Map<String,Object> q);
    @Select("<script>SELECT COUNT(*) FROM rental_tenant"+TENANT_FILTER+"</script>") long tenantCount(Map<String,Object> q);
    @Select("SELECT * FROM rental_tenant WHERE id=#{id} AND deleted=0 FOR UPDATE") Tenant lockTenant(Long id);
    @Insert("INSERT INTO rental_tenant(name,phone,email,remark) VALUES(#{name},#{phone},#{email},#{remark})")
    @Options(useGeneratedKeys=true,keyProperty="id") void addTenant(Map<String,Object> t);
    @Update("UPDATE rental_tenant SET name=#{name},phone=#{phone},email=#{email},remark=#{remark} WHERE id=#{id} AND deleted=0") void editTenant(Map<String,Object> t);
    @Update("UPDATE rental_tenant SET deleted=1 WHERE id=#{id}") void deleteTenant(Long id);
    @Select("SELECT COUNT(*) FROM rental_contract WHERE tenant_id=#{id} AND status='ACTIVE'") long activeTenant(Long id);
    @Select("<script>SELECT c.*,h.title house_title,t.name tenant_name"+CONTRACT_FROM+CONTRACT_FILTER+" ORDER BY c.id DESC LIMIT #{size} OFFSET #{offset}</script>") List<Contract> contracts(Map<String,Object> q);
    @Select("<script>SELECT COUNT(*)"+CONTRACT_FROM+CONTRACT_FILTER+"</script>") long contractCount(Map<String,Object> q);
    @Select("SELECT * FROM rental_contract WHERE id=#{id} FOR UPDATE") Contract lockContract(Long id);
    @Select("SELECT c.*,h.title house_title,t.name tenant_name"+CONTRACT_FROM+" WHERE c.id=#{id}") Contract contract(Long id);
    @Insert("INSERT INTO rental_contract(contract_no,house_id,tenant_id,start_date,end_date,months,monthly_rent,deposit) VALUES(#{contractNo},#{houseId},#{tenantId},#{startDate},#{endDate},#{months},#{monthlyRent},#{deposit})")
    @Options(useGeneratedKeys=true,keyProperty="id") void addContract(Contract c);
    @Update("UPDATE rental_contract SET status='CLOSED',closed_at=NOW() WHERE id=#{id}") void closeContract(Long id);
    @Insert("INSERT INTO rental_bill(contract_id,period,due_date,amount) VALUES(#{contractId},#{period},#{dueDate},#{amount})") void addBill(Bill b);
    @Select("<script>SELECT b.*,c.contract_no,h.title house_title,t.name tenant_name"+BILL_FROM+BILL_FILTER+" ORDER BY b.due_date,b.id LIMIT #{size} OFFSET #{offset}</script>") List<Bill> bills(Map<String,Object> q);
    @Select("<script>SELECT COUNT(*)"+BILL_FROM+BILL_FILTER+"</script>") long billCount(Map<String,Object> q);
    @Select("SELECT * FROM rental_bill WHERE id=#{id} FOR UPDATE") Bill lockBill(Long id);
    @Select("SELECT * FROM rental_bill WHERE id=#{id}") Bill bill(Long id);
    @Select("SELECT * FROM rental_bill WHERE contract_id=#{id} ORDER BY id FOR UPDATE") List<Bill> lockContractBills(Long id);
    @Update("UPDATE rental_bill SET paid_amount=#{paid},status=#{status} WHERE id=#{id}") void billPaid(@Param("id") Long id,@Param("paid") java.math.BigDecimal paid,@Param("status") String status);
    @Update("UPDATE rental_bill SET status='CANCELLED' WHERE contract_id=#{id} AND status='UNPAID'") void cancelFuture(Long id);
    @Select("SELECT * FROM rental_payment WHERE request_id=#{requestId}") Payment paymentByRequest(String requestId);
    @Select("SELECT * FROM rental_payment WHERE bill_id=#{id} ORDER BY id DESC") List<Payment> payments(Long id);
    @Insert("INSERT INTO rental_payment(bill_id,amount,request_id,method,note,actor) VALUES(#{billId},#{amount},#{requestId},#{method},#{note},#{actor})")
    @Options(useGeneratedKeys=true,keyProperty="id") void addPayment(Payment p);
    @Insert("INSERT INTO rental_audit(actor,action,target,request_id) VALUES(#{actor},#{action},#{target},#{requestId})") void audit(Audit a);
    @Select("SELECT * FROM rental_audit ORDER BY id DESC LIMIT #{size} OFFSET #{offset}") List<Audit> audits(@Param("offset") int offset,@Param("size") int size);
    @Select("SELECT COUNT(*) FROM rental_audit") long auditCount();
    @Select("SELECT COUNT(*) houses,COALESCE(SUM(status='VACANT'),0) vacant,COALESCE(SUM(status='OCCUPIED'),0) occupied FROM rental_house WHERE deleted=0") Map<String,Object> houseStats();
    @Select("SELECT COALESCE(SUM(amount-paid_amount),0) receivable,COALESCE(SUM(CASE WHEN due_date < CURDATE() THEN amount-paid_amount ELSE 0 END),0) overdue FROM rental_bill WHERE status IN ('UNPAID','PARTIAL')") Map<String,Object> billStats();
    @Select("SELECT COALESCE(SUM(amount),0) FROM rental_payment WHERE created_at >= DATE_FORMAT(CURDATE(),'%Y-%m-01')") java.math.BigDecimal monthIncome();
    @Select("SELECT COUNT(*) FROM rental_contract WHERE status='ACTIVE'") long activeContracts();
    @Select("SELECT DATE_FORMAT(created_at,'%Y-%m') month,SUM(amount) amount FROM rental_payment GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY month DESC LIMIT 6") List<Map<String,Object>> incomeTrend();
}
