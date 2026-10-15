package com.rentalSys.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin
public class TestController {

    @Resource
    private RedisTemplate redisTemplate;

    @GetMapping("test")
    public String test(){
        redisTemplate.setObject("map", List.of("zs","lisi","ww"),-1L);
        List<String> list = redisTemplate.getObject("map", new TypeReference<>(){});
        log.info(list.toString());
        return "hello ssm"+list;
    }
}
