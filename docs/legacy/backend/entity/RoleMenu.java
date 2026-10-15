package com.rentalSys.entity;

import java.io.Serializable;

/**
 * 角色和菜单关联表(RoleMenu)实体类
 *
 * @author makejava
 * @since 2025-02-04 18:04:12
 */
public class RoleMenu implements Serializable {
    private static final long serialVersionUID = 736886662892327955L;
/**
     * 角色ID
     */
    private Long roleId;
/**
     * 菜单ID
     */
    private Long menuId;


    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

}

