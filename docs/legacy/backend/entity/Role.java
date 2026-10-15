package com.rentalSys.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 角色信息表(Role)实体类
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role implements Serializable {
    private static final long serialVersionUID = 406117762421352792L;

    /**
     * 角色拥有的菜单
     */
    private List<Menu> menus;

/**
     * 角色ID
     */
    private Long roleId;
/**
     * 角色名称
     */
    private String roleName;
/**
     * 角色标签
     */
    private String roleTag;
/**
     * 显示顺序
     */
    private Integer roleSort;
/**
     * 角色状态（0正常 1停用）
     */
    private String status;
/**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;
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

