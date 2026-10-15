package com.rental.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.common.BusinessException;
import com.rental.domain.Models.*;
import com.rental.persistence.AuthMapper;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import javax.annotation.Resource;
import java.security.SecureRandom;
import java.util.*;

/** Redis 精确键定位会话；Lua 原子替换旧会话，不使用 KEYS 扫描。 */
@Service
public class SessionService {
    public static final int TTL = 1800;
    private static final String SESSION = "rental:session:";
    private static final String USER = "rental:user-session:";
    @Resource private JedisPool jedisPool;
    @Resource private ObjectMapper objectMapper;
    @Resource private AuthMapper authMapper;
    @Resource private PasswordHasher hasher;

    public Map<String,Object> login(LoginInput input) {
        String failKey="rental:login-fail:"+input.getUsername().toLowerCase(Locale.ROOT);
        try(Jedis jedis=jedisPool.getResource()) {
            String failures=jedis.get(failKey);
            if(failures!=null && Integer.parseInt(failures)>=5)throw new BusinessException(429,"LOGIN_THROTTLED","登录失败次数过多，请 5 分钟后重试");
        }catch(BusinessException error){throw error;}catch(Exception error){throw unavailable(error);}
        AuthRow row = authMapper.find(input.getUsername());
        if (row == null || !"0".equals(row.getStatus()) || !"0".equals(row.getDelFlag())
                || !hasher.matches(input.getPassword(), row.getPassword())) {
            // INCR 与首次设置 TTL 原子执行，失败计数不会成为永久锁定。
            try(Jedis jedis=jedisPool.getResource()) {
                jedis.eval("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],300) end; return n",
                        Collections.singletonList(failKey),Collections.emptyList());
            }catch(Exception error){throw unavailable(error);}
            throw new BusinessException(401,"BAD_CREDENTIALS","账号或密码错误，或账号已被停用");
        }
        Principal principal = new Principal();
        principal.setId(row.getId()); principal.setUsername(row.getUsername()); principal.setNickname(row.getNickname());
        principal.setRoles(authMapper.roles(row.getId())); principal.setPermissions(permissions(principal.getRoles()));
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try (Jedis jedis = jedisPool.getResource()) {
            // 用同一 Lua 操作完成单用户会话替换和两个键的过期设置。
            String script = "local old=redis.call('GET',KEYS[1]); if old then redis.call('DEL',ARGV[1]..old) end; "
                + "redis.call('SETEX',KEYS[1],ARGV[2],ARGV[3]); redis.call('SETEX',KEYS[2],ARGV[2],ARGV[4]); redis.call('DEL',KEYS[3]); return 1";
            jedis.eval(script, Arrays.asList(USER+row.getId(),SESSION+token,failKey),
                    Arrays.asList(SESSION,String.valueOf(TTL),token,objectMapper.writeValueAsString(principal)));
        } catch (Exception error) { throw unavailable(error); }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("token",token); result.put("expiresIn",TTL); result.put("user",principal);
        return result;
    }
    public Principal resolve(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw expired();
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(SESSION+token);
            if (json == null) throw expired();
            Principal principal = objectMapper.readValue(json,Principal.class);
            // 登录散列计算与管理员禁用可能交错。数据库是账号/角色的权威来源，
            // 请求时再核对一次，防止撤销后才写入的旧身份会话继续访问。
            AuthRow current=authMapper.byId(principal.getId());
            if(current==null || !"0".equals(current.getStatus()) || !"0".equals(current.getDelFlag())
                    || !authMapper.roles(principal.getId()).equals(principal.getRoles())) {
                revoke(jedis,token,principal.getId());
                throw expired();
            }
            // 续期与会话替换在 Redis 中串行执行；旧令牌不能复活。
            Object valid = jedis.eval("if redis.call('GET',KEYS[2])~=ARGV[1] or redis.call('EXISTS',KEYS[1])==0 then return 0 end; "
                    + "redis.call('EXPIRE',KEYS[1],ARGV[2]); redis.call('EXPIRE',KEYS[2],ARGV[2]); return 1",
                    Arrays.asList(SESSION+token,USER+principal.getId()),Arrays.asList(token,String.valueOf(TTL)));
            if (!Long.valueOf(1).equals(valid)) throw expired();
            return principal;
        } catch (BusinessException error) { throw error; }
        catch (Exception error) { throw unavailable(error); }
    }
    public void logout(String token,Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            revoke(jedis,token,userId);
        } catch (Exception error) { throw unavailable(error); }
    }
    // 复用当前连接撤销，避免连接池满时嵌套借用连接。
    private void revoke(Jedis jedis,String token,Long userId) {
        jedis.eval("redis.call('DEL',KEYS[1]); if redis.call('GET',KEYS[2])==ARGV[1] then redis.call('DEL',KEYS[2]) end; return 1",
                Arrays.asList(SESSION+token,USER+userId),Collections.singletonList(token));
    }
    public void invalidate(Long userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.eval("local token=redis.call('GET',KEYS[1]); if token then redis.call('DEL',ARGV[1]..token) end; redis.call('DEL',KEYS[1]); return 1",
                    Collections.singletonList(USER+userId),Collections.singletonList(SESSION));
        } catch (Exception error) { throw unavailable(error); }
    }
    public static Set<String> permissions(Set<String> roles) {
        Set<String> result = new LinkedHashSet<>();
        if (roles.contains("admin")) result.add("*");
        if (roles.contains("operator")) result.addAll(Arrays.asList("rental:read","rental:write","finance:collect"));
        if (roles.contains("viewer")) result.add("rental:read");
        return result;
    }
    private BusinessException expired() { return new BusinessException(401,"SESSION_EXPIRED","登录已过期，请重新登录"); }
    private BusinessException unavailable(Exception error) { return new BusinessException(503,"SESSION_UNAVAILABLE","会话服务暂不可用"); }
}
