package com.rentalSys.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rentalSys.entity.LoginUser;
import com.rentalSys.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;

/**
 * 用户信息表(User)表服务接口
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
public interface UserService {

    /**
     * 通过ID查询单条数据
     *
     * @param userId 主键
     * @return 实例对象
     */
    User queryById(Long userId);

    /**
     * 分页查询
     *
     * @param user 筛选条件
     * @param pageRequest      分页对象
     * @return 查询结果
     */
    Page<User> queryByPage(User user, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    User insert(User user);

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    User update(User user);

    /**
     * 通过主键删除数据
     *
     * @param userId 主键
     * @return 是否成功
     */
    boolean deleteById(Long userId);

    /**
     * 使用用户名和密码登录
     *
     * @param userName
     * @param password
     * @return 是否成功
     */
    LoginUser login(String userName, String password) throws JsonProcessingException;

    void logout();


    /**
     * 获取用户的所有信息
     *
     * @return 用户信息
     */
    HashMap<String, List<String>> getInfo();
}
