package com.shoping.module.auth.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.config.TokenBlacklist;
import com.shoping.module.auth.dto.ChangePasswordRequest;
import com.shoping.module.auth.dto.LoginRequest;
import com.shoping.module.auth.dto.RefreshRequest;
import com.shoping.module.auth.dto.RegisterRequest;
import com.shoping.module.auth.service.AuthService;
import com.shoping.module.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 / 注册 / 刷新 / 登出 / 改密
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.ok(authService.me(AuthContext.requireUserId()));
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(AuthContext.requireUserId(), request);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 将当前令牌与刷新令牌加入黑名单，实现服务端失效
        String accessToken = resolveToken(request);
        if (accessToken != null) {
            tokenBlacklist.add(accessToken);
        }
        return Result.ok();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
