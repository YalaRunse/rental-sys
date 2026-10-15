package com.rental.auth;

import com.rental.common.BusinessException;
import com.rental.domain.Models.Principal;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** 先认证，再读取服务端权限注解；请求体不能自报用户身份或角色。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Resource private SessionService sessions;
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        Principal principal = sessions.resolve(token);
        request.setAttribute("principal",principal); request.setAttribute("token",token);
        HandlerMethod method = (HandlerMethod)handler;
        Require permission = method.getMethodAnnotation(Require.class);
        if (permission == null) permission = method.getBeanType().getAnnotation(Require.class);
        if (permission != null && !principal.getPermissions().contains("*") && !principal.getPermissions().contains(permission.value()))
            throw new BusinessException(403,"FORBIDDEN","当前账号没有此操作权限");
        return true;
    }
}
