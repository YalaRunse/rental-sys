package com.rentalSys.controller;

import com.rentalSys.annotation.HasPermission;
import com.rentalSys.entity.User;
import com.rentalSys.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * 用户信息表(User)表控制层
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
@RestController
@RequestMapping("user")
public class UserController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private UserService userService;

    /**
     * 分页查询
     *
     * @param user 筛选条件
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<User>> queryByPage(User user) {
        return ResponseEntity.ok(this.userService.queryByPage(user, PageRequest.of(user.getPage(), user.getSize())));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    @HasPermission("system:user:query")
    public ResponseEntity<User> queryById(@PathVariable("id") Long id) {
//        //system:user:query    admin   hr
//        LoginUser loginUser = getLoginUser();
//        //获取用户，根据用户查询他的角色或者权限，去判断接口所需要的权限是否包含在用户所拥有的角色当中
//        List<String> hasPerms = redisTemplate.getObject(Constants.PERMS_PREFIX + loginUser.getToken(), new TypeReference<>() {
//        });
//
//        if (!hasPerms.contains("system:user:query")) {
//            throw new RuntimeException("没有权限");
//        }

        return ResponseEntity.ok(this.userService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param user 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<User> add(User user) {
        return ResponseEntity.ok(this.userService.insert(user));
    }

    /**
     * 编辑数据
     *
     * @param user 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<User> edit(User user) {
        return ResponseEntity.ok(this.userService.update(user));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userService.deleteById(id));
    }


    @GetMapping("getInfo")
    public ResponseEntity<HashMap<String, List<String>>>getInfo() {
        return ResponseEntity.ok(this.userService.getInfo());
    }
}

