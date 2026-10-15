package com.rentalSys.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rentalSys.entity.LoginUser;
import com.rentalSys.entity.User;
import com.rentalSys.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
public class IndexController {

//    @Resource
//    private User user;
    @Resource
    private UserServiceImpl userService;

    @PostMapping("login")
    public ResponseEntity<LoginUser> login(@RequestBody @Validated User user, BindingResult bindingResult){
        //1.处理不合法的数据
        List<ObjectError> allErrors = bindingResult.getAllErrors();
        allErrors.forEach(error -> log.error("登录时用户校验失败：{}",error.getDefaultMessage()));
        if (allErrors.size()>0){
            return ResponseEntity.status(500).build();
        }

        //执行登录逻辑

        LoginUser loginUser = null;
        try {
            loginUser = userService.login(user.getUserName(),user.getPassword());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().body(loginUser);
    }


    @GetMapping("logout")
    public ResponseEntity<String> logout(){

        //执行退出登录逻辑
        try {
            userService.logout();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().body("退出成功");
    }
}
