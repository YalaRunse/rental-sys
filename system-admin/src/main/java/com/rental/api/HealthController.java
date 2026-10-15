package com.rental.api;

import com.rental.common.Api;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;

/** 探活实际访问数据库和 Redis，不仅检查 Java 进程是否存在。 */
@RestController
public class HealthController {
    @Resource private DataSource dataSource;
    @Resource private JedisPool jedisPool;
    @GetMapping("/api/health") public Api<Map<String,Object>> health() {
        new JdbcTemplate(dataSource).queryForObject("SELECT 1",Integer.class);
        try(Jedis jedis=jedisPool.getResource()){if(!"PONG".equals(jedis.ping()))throw new IllegalStateException("Redis 探活失败");}
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","UP");result.put("service","rentalSys");return Api.ok(result);
    }
}
