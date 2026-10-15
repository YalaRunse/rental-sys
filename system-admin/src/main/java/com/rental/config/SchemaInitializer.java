package com.rental.config;

import com.rental.auth.PasswordHasher;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/** 版本化增量初始化。DDL 可重复执行；记录完成版本，绝不删除旧数据。 */
@Component
public class SchemaInitializer {
    @Resource private DataSource dataSource;
    @Resource private PasswordHasher hasher;
    @PostConstruct public void initialize() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS rental_schema_history(version INT PRIMARY KEY,applied_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM rental_schema_history WHERE version=1",Integer.class)==0) {
            try (Connection connection = dataSource.getConnection()) { ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/V1__portfolio.sql")); }
            jdbc.update("INSERT INTO rental_schema_history(version) VALUES(1)");
        }
        // 原 demo 的默认用户保留；新数据库才创建示例管理员。
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE user_name='admin'",Integer.class)==0)
            jdbc.update("INSERT INTO user(user_name,nick_name,password,status,del_flag) VALUES('admin','管理员',?,'0','0')",hasher.hash("111111"));
        // 迁移历史明文密码，之后认证只接受散列格式。迁移本身不修改原密码。
        List<Map<String,Object>> users = jdbc.queryForList("SELECT user_id,password FROM user WHERE password NOT LIKE 'pbkdf2$%'");
        for (Map<String,Object> user:users) jdbc.update("UPDATE user SET password=? WHERE user_id=?",hasher.hash(String.valueOf(user.get("password"))),user.get("user_id"));
        String[][] roles={{"admin","管理员"},{"operator","租赁运营"},{"viewer","只读观察员"}};
        for (String[] role:roles) {
            jdbc.update("INSERT INTO role(role_name,role_tag,role_sort,status,del_flag) SELECT ?,?,1,'0','0' WHERE NOT EXISTS(SELECT 1 FROM role WHERE role_tag=?)",role[1],role[0],role[0]);
        }
        jdbc.update("INSERT IGNORE INTO user_role(user_id,role_id) SELECT u.user_id,r.role_id FROM user u JOIN role r ON r.role_tag='admin' WHERE u.user_name='admin'");
        // 唯一用户名约束与数据库现有数据先核对，重复数据时明确阻止启动。
        boolean uniqueName = false;
        for (Map<String,Object> index:jdbc.queryForList("SHOW INDEX FROM user"))
            if ("uk_portfolio_username".equals(index.get("Key_name"))) uniqueName=true;
        if (!uniqueName) jdbc.execute("ALTER TABLE user ADD UNIQUE KEY uk_portfolio_username(user_name)");
    }
}
