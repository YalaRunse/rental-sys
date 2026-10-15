package com.rental.service;

import com.rental.auth.*;
import com.rental.common.*;
import com.rental.domain.Models.*;
import com.rental.persistence.AuthMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;

/** 管理员维护账号与角色；角色变更必须使旧会话失效。 */
@Service
public class AdminService {
    @Resource private AuthMapper mapper;
    @Resource private PasswordHasher hasher;
    @Resource private SessionService sessions;
    @Resource private RentalService rental;
    public Page<UserView> users(String query,int page,int size) {
        int offset=Page.offset(page,size);
        if(query!=null && query.length()>100)throw BusinessException.bad("查询词最多 100 字符");
        List<UserView> users=mapper.users(query,offset,size);
        for(UserView user:users)user.setRoles(new ArrayList<>(mapper.roles(user.getId())));
        return new Page<>(users,mapper.count(query),page,size);
    }
    public List<RoleView> roles() {
        List<RoleView> roles=mapper.allRoles();
        for(RoleView role:roles)role.setPermissions(SessionService.permissions(Collections.singleton(role.getTag())));
        return roles;
    }
    @Transactional public Long create(UserInput input,String actor) {
        Long role=mapper.roleId(input.getRole());if(role==null)throw BusinessException.bad("角色不存在");
        Map<String,Object> row=new HashMap<>();row.put("username",input.getUsername());row.put("nickname",input.getNickname());
        row.put("email",input.getEmail());row.put("phone",input.getPhone());row.put("password",hasher.hash(input.getPassword()));
        mapper.insert(row);Long id=((Number)row.get("id")).longValue();mapper.assign(id,role);
        rental.audit(actor,"USER_CREATE",String.valueOf(id));return id;
    }
    @Transactional public void access(Long id,UserAccessInput input,Principal actor) {
        if(id.equals(actor.getId()))throw BusinessException.conflict("不能调整当前登录账号的角色或状态");
        AuthRow user=mapper.byId(id);if(user==null || !"0".equals(user.getDelFlag()))throw BusinessException.missing("用户不存在");
        Long role=mapper.roleId(input.getRole());if(role==null)throw BusinessException.bad("角色不存在");
        mapper.access(id,input.getStatus());mapper.clearRoles(id);mapper.assign(id,role);
        sessions.invalidate(id);rental.audit(actor.getUsername(),"USER_ACCESS_CHANGE",String.valueOf(id));
    }
}
