package com.factory.erp.auth;

import com.factory.erp.common.BaseService;
import com.factory.erp.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户认证 Service。
 * 管理用户账号，提供登录验证和简单 token 生成。
 * 第一版支持三种角色：老板、仓管、销售。
 */
@Service
public class AuthService extends BaseService {

    private final Map<String, UserRecord> users = new LinkedHashMap<>();

    public AuthService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @PostConstruct
    void init() {
        createSchema();
        loadFromDatabase();
        seedIfNeeded();
    }

    /**
     * 登录验证。前端传来的 password 已经是 MD5 值。
     */
    public synchronized UserInfo login(String username, String password) {
        UserRecord user = users.get(username);
        if (user == null || !user.passwordHash().equals(hashPassword(username, password))) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = generateToken(user);
        return new UserInfo(user.username(), user.displayName(), user.role(), token);
    }

    /**
     * 根据 token 验证用户身份。
     */
    public synchronized UserInfo validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        // 简单 token 格式: base64(username:timestamp)
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String username = decoded.split(":")[0];
            UserRecord user = users.get(username);
            if (user != null) {
                return new UserInfo(user.username(), user.displayName(), user.role(), token);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void seedIfNeeded() {
        // 开发阶段：每次启动都重置用户表，确保默认账号可用
        jdbcTemplate.update("delete from users");
        users.clear();
        addUser("admin", "admin123", "系统管理员", "老板");
        addUser("warehouse", "wh123", "仓管员", "仓管");
        addUser("sales", "sales123", "销售员", "销售");
    }

    private void addUser(String username, String rawPassword, String displayName, String role) {
        // 模拟前端 MD5 后再 sha256(username + md5)
        String md5Password = md5(rawPassword);
        String hash = hashPassword(username, md5Password);
        UserRecord user = new UserRecord(username, hash, displayName, role);
        users.put(username, user);
        jdbcTemplate.update(
                "insert into users (username, password_hash, display_name, role) values (?, ?, ?, ?)",
                username, hash, displayName, role
        );
    }

    private String generateToken(UserRecord user) {
        String raw = user.username() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String hashPassword(String username, String md5Password) {
        return sha256(username + md5Password);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                create table if not exists users (
                    username text primary key,
                    password_hash text not null,
                    display_name text not null,
                    role text not null
                )
                """);
    }

    private void loadFromDatabase() {
        users.clear();
        jdbcTemplate.query("select username, password_hash, display_name, role from users order by username", (java.sql.ResultSet rs) -> {
            while (rs.next()) {
                UserRecord user = new UserRecord(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("role")
                );
                users.put(user.username(), user);
            }
        });
    }

    public record UserRecord(String username, String passwordHash, String displayName, String role) {}
    public record UserInfo(String username, String displayName, String role, String token) {}
}
