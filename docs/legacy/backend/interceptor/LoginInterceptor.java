package com.rentalSys.interceptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.configuration.CustomObjectMapper;
import com.rentalSys.constant.Constants;
import com.rentalSys.core.RedisTemplate;
import com.rentalSys.entity.LoginUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Set;

public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private CustomObjectMapper customObjectMapper;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        //2.如果不是白名单的请求，检测
        ResponseEntity<String> res = ResponseEntity.status(401).body("Bad Credentials");
        String token = request.getHeader(Constants.HEAD_AUTHORIZATION);
        //拿到首部信息的Authorization请求头，拿到首部信息的Authorization的值

        //3.使用token去redis中查看

        if (token == null){
            response.setStatus(401);
            response.getWriter().write(customObjectMapper.writeValueAsString(res));
            return false;
        }

        Set<String> loginToken = redisTemplate.keys(Constants.TOKEN_PREFIX + "*" + token);
        //如果token不存在，返回401
        if (loginToken == null || loginToken.size() == 0){
            response.setStatus(401);
            response.getWriter().write(customObjectMapper.writeValueAsString(res));
            return false;
        }
        String tokenKey = loginToken.iterator().next();
        LoginUser loginUser = redisTemplate.getObject(tokenKey, new TypeReference<>() {
        });

        if (Objects.isNull(loginUser)){
            response.setStatus(401);
            response.getWriter().write(customObjectMapper.writeValueAsString(res));
            return false;
        }

        // 登录键已变成 token:用户名:令牌，续期必须使用刚才实际找到的键。
        redisTemplate.expire(tokenKey, Constants.TOKEN_TIME);
        // 角色和权限缓存跟随有效会话续期，避免使用中途只剩登录状态。
        redisTemplate.expire(Constants.ROLE_PREFIX + token, Constants.TOKEN_TIME);
        redisTemplate.expire(Constants.PERMS_PREFIX + token, Constants.TOKEN_TIME);


        return true;
    }
}
