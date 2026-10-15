package com.rental.persistence;

import com.rental.domain.Models.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Set;

/** 用户认证与管理使用显式列清单，列表查询不读取 password。 */
public interface AuthMapper {
    @Select("SELECT user_id id,user_name username,nick_name nickname,password,status,del_flag FROM user WHERE user_name=#{username} LIMIT 1")
    AuthRow find(String username);
    @Select("SELECT user_id id,user_name username,nick_name nickname,password,status,del_flag FROM user WHERE user_id=#{id}")
    AuthRow byId(Long id);
    @Update("UPDATE user SET password=#{password},update_time=NOW() WHERE user_id=#{id}")
    void password(@Param("id") Long id,@Param("password") String password);
    @Select("SELECT DISTINCT r.role_tag FROM role r JOIN user_role ur ON r.role_id=ur.role_id WHERE ur.user_id=#{id} AND r.status='0' AND r.del_flag='0' AND r.role_tag IS NOT NULL")
    Set<String> roles(Long id);
    @Select("SELECT role_id id,role_name name,role_tag tag FROM role WHERE del_flag='0' AND role_tag IN ('admin','operator','viewer') ORDER BY role_sort,role_id")
    List<RoleView> allRoles();
    @Select({"<script>SELECT user_id id,user_name username,nick_name nickname,email,phonenumber phone,status FROM user WHERE del_flag='0'",
        "<if test='query != null and query != &quot;&quot;'>AND (user_name LIKE CONCAT('%',#{query},'%') OR nick_name LIKE CONCAT('%',#{query},'%'))</if>",
        "ORDER BY user_id LIMIT #{size} OFFSET #{offset}</script>"})
    List<UserView> users(@Param("query") String query,@Param("offset") int offset,@Param("size") int size);
    @Select({"<script>SELECT COUNT(*) FROM user WHERE del_flag='0'",
        "<if test='query != null and query != &quot;&quot;'>AND (user_name LIKE CONCAT('%',#{query},'%') OR nick_name LIKE CONCAT('%',#{query},'%'))</if></script>"})
    long count(String query);
    @Insert("INSERT INTO user(user_name,nick_name,email,phonenumber,password,status,del_flag,create_time) VALUES(#{username},#{nickname},#{email},#{phone},#{password},'0','0',NOW())")
    @Options(useGeneratedKeys=true,keyProperty="id")
    void insert(java.util.Map<String,Object> values);
    @Select("SELECT role_id FROM role WHERE role_tag=#{tag} AND status='0' AND del_flag='0' LIMIT 1")
    Long roleId(String tag);
    @Delete("DELETE FROM user_role WHERE user_id=#{id}") void clearRoles(Long id);
    @Insert("INSERT INTO user_role(user_id,role_id) VALUES(#{userId},#{roleId})")
    void assign(@Param("userId") Long userId,@Param("roleId") Long roleId);
    @Update("UPDATE user SET status=#{status},update_time=NOW() WHERE user_id=#{id} AND del_flag='0'")
    void access(@Param("id") Long id,@Param("status") String status);
}
