package com.rentalSys.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.configuration.CustomObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Resource;
import java.util.Set;

@Slf4j
@Component
public class RedisTemplate {
    @Resource
    private JedisPool jedisPool;

    @Resource
    private CustomObjectMapper customObjectMapper;

    //保存字符串类型的数据
    public String set(String key, String value,Long expire){
        Jedis jedis= jedisPool.getResource();
        String returnValue = null;
        try {
            returnValue = jedis.setex(key,expire,value);
        }catch (JedisException jedisException){
            log.error("redis错误",jedisException);
            jedisPool.returnBrokenResource(jedis);
    }finally {
            jedisPool.returnResource(jedis);
        }
        return returnValue;
    }

    //保存字符串类型的数据
    public String get(String key){
        Jedis jedis= jedisPool.getResource();
        String returnValue = null;
        try {
            returnValue = jedis.get(key);
        }catch (JedisException jedisException){
            log.error("redis错误",jedisException);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return returnValue;
    }

    //删除数据
    public Long remove(String key){
        Jedis jedis= jedisPool.getResource();
        Long del = 0L;
        try {
            del = jedis.del(key);
        }catch (JedisException jedisException){
            log.error("redis错误",jedisException);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return del;
    }

    //将对象以序列化的方式存在redis中，json
    //保存字符串类型的数据
    public String setObject(String key, Object value,Long expire){
        Jedis jedis= jedisPool.getResource();
        String returnValue = null;
        try {
            String jsonValue = customObjectMapper.writeValueAsString(value);
            if (expire<0){
                returnValue = jedis.set(key,jsonValue);
            }else {
                returnValue = jedis.setex(key,expire,jsonValue);
            }
        }catch (JedisException | JsonProcessingException exception){
            log.error("redis错误",exception);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return returnValue;
    }

    public <T> T getObject(String key, TypeReference<T> typeReference){
        Jedis jedis= jedisPool.getResource();
        String returnValue = null;
        T object = null;
        try {
            //从redis中获取
            returnValue = jedis.get(key);
            // Redis 缓存不存在是正常情况，不能把 null 交给 Jackson 解析。
            if (returnValue == null) {
                return null;
            }
            object = customObjectMapper.readValue(returnValue,typeReference);
        }catch (JedisException | JsonProcessingException exception){
            log.error("redis错误",exception);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return object;
    }

    //增加key的时间
    public Long expire(String key, Long expire){
        Jedis jedis= jedisPool.getResource();
        Long exp = -1L;
        try {
            //从redis中获取
            exp = jedis.expire(key, expire);
        }catch (JedisException exception){
            log.error("redis错误",exception);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return exp;
    }

    //keys进行查询特定的key
    public Set<String> keys(String pattern){
        Jedis jedis= jedisPool.getResource();
        Set<String> keys = null;
        try {
            //从redis中获取
            keys = jedis.keys(pattern);
        }catch (JedisException exception){
            log.error("redis错误",exception);
            jedisPool.returnBrokenResource(jedis);
        }finally {
            jedisPool.returnResource(jedis);
        }
        return keys;
    }
}
