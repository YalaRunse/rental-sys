package com.rentalSys.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.rentalSys.configuration.CustomObjectMapper;
import com.rentalSys.constant.Constants;
import com.rentalSys.core.RedisTemplate;
import com.rentalSys.dao.UserDao;
import com.rentalSys.entity.LoginUser;
import com.rentalSys.entity.Menu;
import com.rentalSys.entity.Role;
import com.rentalSys.entity.User;
import com.rentalSys.exception.PasswordIncorrectException;
import com.rentalSys.exception.UserNotFoundException;
import com.rentalSys.service.UserService;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户信息表(User)表服务实现类
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
@Slf4j
@Service("userService")
public class UserServiceImpl implements UserService {
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private UserDao userDao;

    @Resource
    private CustomObjectMapper customObjectMapper;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 通过ID查询单条数据
     *
     * @param userId 主键
     * @return 实例对象
     */
    @Override
    public User queryById(Long userId) {
        return this.userDao.queryById(userId);
    }

    /**
     * 分页查询
     *
     * @param user 筛选条件
     * @param pageRequest      分页对象
     * @return 查询结果
     */
    @Override
    public Page<User> queryByPage(User user, PageRequest pageRequest) {
        long total = this.userDao.count(user);
        return new PageImpl<>(this.userDao.queryAllByLimit(user, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    @Override
    public User insert(User user) {
        this.userDao.insert(user);
        return user;
    }

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    @Override
    public User update(User user) {
        this.userDao.update(user);
        return this.queryById(user.getUserId());
    }

    /**
     * 通过主键删除数据
     *
     * @param userId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long userId) {
        return this.userDao.deleteById(userId) > 0;
    }

    @Override
    public LoginUser login(String userName, String password) throws JsonProcessingException {
        //登录，使用用户名查询用户
        User user = userDao.queryByUserName(userName);
        //情况1 没有查询到，没有该账号
        if (user == null){
            throw new UserNotFoundException("用户【"+userName+"】不存在 ");
        }
        //情况2 如果查询到了，比较密码，密码不正确登录失败
        if (!password.equals(user.getPassword())){
            log.info("用户【"+userName+"】密码输入错误");
            throw new PasswordIncorrectException("用户【"+userName+"】密码输入错误 ");
        }
        //情况3 如果账号密码正确

        //1）生成token
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        UserAgent userAgent = new UserAgent(request.getHeader("User-Agent"));
        String token = UUID.randomUUID().toString();

        // 通过ip获取其所属的地址
        ResponseEntity<String> result = restTemplate.getForEntity("https://whois.pconline.com.cn/ipJson.jsp?ip="+request.getRemoteHost()+"&json=true", String.class);
        String body = result.getBody();
        Map<String,String> map = customObjectMapper.readValue(body, new TypeReference<>() {});
        String location = map.get("addr")+map.get("pro")+map.get("city")+map.get("region");

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getUserId())
                .token(token)
                .ipaddr(request.getRemoteAddr())
                .loginTime(new Date())
                .os(userAgent.getOperatingSystem().getName())
                .browser(userAgent.getBrowser().getName())
                .loginLocation(location)
                .user(user)
                .build();
        //2）封装一个LoginUser，保存在redis
        // 对key进行处理 token:username:uuid

        //根据用户名生成key前缀
        String keyPrefix = Constants.TOKEN_PREFIX+userName+":";
        // 查询前缀 token:username: 的数据
        // 根据生成的key的前缀进行判断 token:username:
        redisTemplate.keys(keyPrefix+"*").forEach(key->{
            // 删除之前的登录信息
            redisTemplate.remove(key);
        });

        //把新的数据加入redis
        redisTemplate.setObject(keyPrefix+token,loginUser,Constants.TOKEN_TIME);
        return loginUser;
    }

    @Override
    public void logout() {

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        String token = request.getHeader(Constants.HEAD_AUTHORIZATION);
        // 与登录时写入的 token:用户名:令牌 格式保持一致，真正使当前会话失效。
        LoginUser loginUser = getLoginUser();
        redisTemplate.remove(Constants.TOKEN_PREFIX + loginUser.getUser().getUserName() + ":" + token);
        redisTemplate.remove(Constants.ROLE_PREFIX + token);
        redisTemplate.remove(Constants.PERMS_PREFIX + token);
    }

    @Override
    public HashMap<String, List<String>> getInfo() {
        //1.获取当前登录的对象
        LoginUser loginUser = getLoginUser();
        //2.查询当前用户的角色和权限
        User info = userDao.getInfo(loginUser.getUserId());
        //3.处理权限和角色的相关信息
        //1)roles:token:[admin,xxx,yyy] perms:token:[system:user:add,system:user:update]
        // 没有关联角色时返回空集合，登录状态本身不代表拥有任何角色或权限。
        List<Role> roles = info == null || info.getRoles() == null
                ? Collections.emptyList() : info.getRoles();
        List<String> roleTags = roles.stream().map(Role::getRoleTag)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        redisTemplate.setObject(Constants.ROLE_PREFIX+loginUser.getToken(),roleTags,Constants.TOKEN_TIME);

        List<String> perms = new ArrayList<>();
//        info.getRoles().stream().map(Role::getMenus).forEach(menus -> {
//            menus.forEach(menu -> {
//                perms.add(menu.getPerms());
//            });
//        });

        roles.stream().map(Role::getMenus).filter(Objects::nonNull).forEach(menus -> {
            // 空权限标识不进入权限缓存，保留原来的精确匹配规则。
            perms.addAll(menus.stream().map(Menu::getPerms).filter(Objects::nonNull)
                    .filter(permission -> !permission.isEmpty()).distinct().collect(Collectors.toList()));
        });

        redisTemplate.setObject(Constants.PERMS_PREFIX+loginUser.getToken(),perms,Constants.TOKEN_TIME);

        //整合数据
        HashMap<String, List<String>> data = new HashMap<>();
        data.put("roles",roleTags);
        data.put("perms",perms);

        return data;
    }

    private LoginUser getLoginUser(){

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
