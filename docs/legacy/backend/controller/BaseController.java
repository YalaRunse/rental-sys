package com.rentalSys.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.constant.Constants;
import com.rentalSys.core.RedisTemplate;
import com.rentalSys.entity.LoginUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Set;

public class BaseController {

    @Resource
    RedisTemplate redisTemplate;

    protected LoginUser getLoginUser(){

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String token = request.getHeader(Constants.HEAD_AUTHORIZATION);
        //拿到首部信息的Authorization请求头，拿到首部信息的Authorization的值

        //3.使用token去redis中查看

        if (token == null){
            throw new RuntimeException("当前用户未登录");
        }

        Set<String> loginToken = redisTemplate.keys(Constants.TOKEN_PREFIX + "*" + token);
        //如果token不存在，返回401
        if (loginToken == null || loginToken.size() == 0){
            throw new RuntimeException("当前用户未登录");
        }
        String tokenKey = loginToken.iterator().next();
        return redisTemplate.getObject(tokenKey, new TypeReference<>() {});
    }

}
