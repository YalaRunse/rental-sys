package com.rentalSys.aspect;


import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.annotation.HasPermission;
import com.rentalSys.annotation.HasRole;
import com.rentalSys.constant.Constants;
import com.rentalSys.core.RedisTemplate;
import com.rentalSys.exception.NoPermissionInfoException;
import com.rentalSys.exception.NoRoleInfoException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@Component
@Aspect
public class PermissionAspect {

    // 切面也需要由 Spring 注入 Redis 工具，否则调用权限接口会发生空指针。
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 执行接口角色所需角色的切面
     * @param joinPoint
     * @param hasRole
     */
    @Before("@annotation(hasRole)")
    public void roleBefore(JoinPoint joinPoint, HasRole hasRole) {
        //获得当前方法所需要的角色
        String[] needRoles = hasRole.value();
        //获得拥有的角色
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String token = request.getHeader(Constants.HEAD_AUTHORIZATION);
        List<String> hasRoles = redisTemplate.getObject(Constants.ROLE_PREFIX + token, new TypeReference<>() {
        });

        //只要所需的角色有一个在你拥有的角色中就放行
        boolean flag = false;
        for (String needRole : needRoles) {
            // 权限缓存尚未建立或已过期时拒绝访问，避免空指针导致 500。
            if (hasRoles != null && hasRoles.contains(needRole)) {
                flag = true;
            }
        }
        if (!flag) {
            throw new NoRoleInfoException("没有所需角色");
        }
    }
    @Before("@annotation(hasPermission)")
    public void roleBefore(JoinPoint joinPoint, HasPermission hasPermission) {
        //获得当前方法所需要的权限
        String[] needPermissions = hasPermission.value();
        //获得拥有的权限
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String token = request.getHeader(Constants.HEAD_AUTHORIZATION);
        List<String> hasPerms = redisTemplate.getObject(Constants.PERMS_PREFIX + token, new TypeReference<>() {
        });

        //只要所需的权限有一个在你拥有的权限中就放行
        boolean flag = false;
        for (String needPermission : needPermissions) {
            // 缺少权限缓存不能视为拥有权限，按无权限处理。
            if (hasPerms != null && hasPerms.contains(needPermission)) {
                flag = true;
            }
        }
        if (!flag) {
            throw new NoPermissionInfoException("没有所需权限");
        }
    }
}
