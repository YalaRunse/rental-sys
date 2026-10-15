package com.rental.api;

import com.rental.auth.Require;
import com.rental.common.*;
import com.rental.domain.Models.*;
import com.rental.service.RentalService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

/** HTTP 层只负责参数绑定和权限声明，事务与金额规则在服务层。 */
@RestController
@RequestMapping("/api")
public class RentalController {
    @Resource private RentalService service;
    @GetMapping("/dashboard") @Require("rental:read") public Api<Map<String,Object>> dashboard(){return Api.ok(service.dashboard());}
    @GetMapping("/houses") @Require("rental:read") public Api<Page<House>> houses(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(service.houses(query,status,page,size));}
    @PostMapping("/houses") @Require("rental:write") public Api<House> addHouse(@Valid @RequestBody HouseInput input,@RequestAttribute Principal principal){return Api.ok(service.saveHouse(null,input,principal.getUsername()));}
    @PutMapping("/houses/{id}") @Require("rental:write") public Api<House> editHouse(@PathVariable Long id,@Valid @RequestBody HouseInput input,@RequestAttribute Principal principal){return Api.ok(service.saveHouse(id,input,principal.getUsername()));}
    @DeleteMapping("/houses/{id}") @Require("rental:write") public Api<Void> deleteHouse(@PathVariable Long id,@RequestAttribute Principal principal){service.deleteHouse(id,principal.getUsername());return Api.ok(null);}
    @GetMapping("/tenants") @Require("rental:read") public Api<Page<Tenant>> tenants(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(service.tenants(query,page,size));}
    @PostMapping("/tenants") @Require("rental:write") public Api<Tenant> addTenant(@Valid @RequestBody TenantInput input,@RequestAttribute Principal principal){return Api.ok(service.saveTenant(null,input,principal.getUsername()));}
    @PutMapping("/tenants/{id}") @Require("rental:write") public Api<Tenant> editTenant(@PathVariable Long id,@Valid @RequestBody TenantInput input,@RequestAttribute Principal principal){return Api.ok(service.saveTenant(id,input,principal.getUsername()));}
    @DeleteMapping("/tenants/{id}") @Require("rental:write") public Api<Void> deleteTenant(@PathVariable Long id,@RequestAttribute Principal principal){service.deleteTenant(id,principal.getUsername());return Api.ok(null);}
    @GetMapping("/contracts") @Require("rental:read") public Api<Page<Contract>> contracts(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(service.contracts(query,status,page,size));}
    @PostMapping("/contracts") @Require("rental:write") public Api<Contract> sign(@Valid @RequestBody ContractInput input,@RequestAttribute Principal principal){return Api.ok(service.sign(input,principal.getUsername()));}
    @PostMapping("/contracts/{id}/close") @Require("rental:write") public Api<Contract> close(@PathVariable Long id,@RequestAttribute Principal principal){return Api.ok(service.close(id,principal.getUsername()));}
    @GetMapping("/bills") @Require("rental:read") public Api<Page<Bill>> bills(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="")String status,@RequestParam(required=false)Long contractId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(service.bills(query,status,contractId,page,size));}
    @GetMapping("/bills/{id}/payments") @Require("rental:read") public Api<List<Payment>> payments(@PathVariable Long id){return Api.ok(service.payments(id));}
    @PostMapping("/bills/{id}/payments") @Require("finance:collect") public Api<Payment> pay(@PathVariable Long id,@Valid @RequestBody PaymentInput input,@RequestAttribute Principal principal){return Api.ok(service.pay(id,input,principal.getUsername()));}
}
