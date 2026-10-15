package com.rental.api;

import com.rental.auth.Require;
import com.rental.common.*;
import com.rental.domain.Models.*;
import com.rental.service.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/** 系统管理接口统一要求管理员权限，普通业务角色不能配置自己的权限。 */
@RestController
@RequestMapping("/api/system")
@Require("system:manage")
public class AdminController {
    @Resource private AdminService service;
    @Resource private RentalService rental;
    @GetMapping("/users") public Api<Page<UserView>> users(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(service.users(query,page,size));}
    @PostMapping("/users") public Api<Long> create(@Valid @RequestBody UserInput input,@RequestAttribute Principal principal){return Api.ok(service.create(input,principal.getUsername()));}
    @PutMapping("/users/{id}/access") public Api<Void> access(@PathVariable Long id,@Valid @RequestBody UserAccessInput input,@RequestAttribute Principal principal){service.access(id,input,principal);return Api.ok(null);}
    @GetMapping("/roles") public Api<List<RoleView>> roles(){return Api.ok(service.roles());}
    @GetMapping("/audits") public Api<Page<Audit>> audits(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return Api.ok(rental.audits(page,size));}
}
