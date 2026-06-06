package com.factory.erp.auth;

import com.factory.erp.common.BaseService;
import com.factory.erp.common.exception.BusinessException;
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

    public synchronized void reloadFromDatabase() {
        loadFromDatabase();
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

    private void loadFromDatabase() {
        users.clear();
        jdbcTemplate.query("select username, password_hash, display_name, role from users order by username", (java.sql.ResultSet rs) -> {
            UserRecord user = new UserRecord(
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("display_name"),
                    rs.getString("role")
            );
            users.put(user.username(), user);
        });
    }

    public record UserRecord(String username, String passwordHash, String displayName, String role) {}
    public record UserInfo(String username, String displayName, String role, String token) {}
}
