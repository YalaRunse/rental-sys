package com.rental.api;

import com.rental.auth.SessionService;
import com.rental.common.Api;
import com.rental.domain.Models.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** 认证接口的响应只包含安全用户信息，不返回密码或密码散列。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource private SessionService sessions;
    @PostMapping("/login") public Api<Map<String,Object>> login(@Valid @RequestBody LoginInput input) {return Api.ok(sessions.login(input));}
    @GetMapping("/me") public Api<Principal> me(@RequestAttribute Principal principal) {return Api.ok(principal);}
    @PostMapping("/logout") public Api<Void> logout(HttpServletRequest request,@RequestAttribute Principal principal) {
        sessions.logout((String)request.getAttribute("token"),principal.getId());return Api.ok(null);
    }
}
