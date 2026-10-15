package com.rentalSys.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.io.Serializable;
import java.util.List;

/**
 * 用户信息表(User)实体类
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -41470355775666285L;

    /**
     * 用户拥有的角色
     */
    private List<Role> roles;




/**
     * 用户ID
     */
    private Long userId;
/**
     * 用户账号
     */
@NotNull
@Size(min = 5,max = 20,message = "用户名长度应为5-20位字符")
    private String userName;
/**
     * 用户昵称
     */
    private String nickName;
/**
     * 用户邮箱
     */
@Email
    private String email;
/**
     * 手机号码
     */
@Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phonenumber;
/**
     * 头像地址
     */
    private String avatar;
/**
     * 密码
     */
@NotNull
@Size(min = 5,max = 20,message = "密码长度应为5-20位字符")
    private String password;
/**
     * 帐号状态（0正常 1停用）
     */
    private String status;
/**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;
/**
     * 最后登录IP
     */
    private String loginIp;
/**
     * 最后登录时间
     */
    private Date loginDate;
/**
     * 创建者
     */
    private String createBy;
/**
     * 创建时间
     */
    private Date createTime;
/**
     * 更新者
     */
    private String updateBy;
/**
     * 更新时间
     */
    private Date updateTime;


}

