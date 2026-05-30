package com.factory.erp.auth;

import com.factory.erp.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证 API。
 * 提供登录验证和当前用户信息查询接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 用户登录，返回 token 和用户信息。 */
    @PostMapping("/login")
    public ApiResponse<AuthService.UserInfo> login(@Valid @RequestBody LoginRequest request) {
        AuthService.UserInfo info = authService.login(request.username(), request.password());
        return ApiResponse.ok(info);
    }

    /** 根据 token 获取当前用户信息。 */
    @GetMapping("/me")
    public ApiResponse<AuthService.UserInfo> me(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        AuthService.UserInfo info = authService.validateToken(token);
        if (info == null) {
            return ApiResponse.error("未登录或 token 已失效");
        }
        return ApiResponse.ok(info);
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }
}
