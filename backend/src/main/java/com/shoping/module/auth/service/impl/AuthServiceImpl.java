package com.shoping.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.config.JwtUtil;
import com.shoping.config.LoginRateLimiter;
import com.shoping.config.TokenBlacklist;
import com.shoping.module.auth.dto.ChangePasswordRequest;
import com.shoping.module.auth.dto.LoginRequest;
import com.shoping.module.auth.dto.RefreshRequest;
import com.shoping.module.auth.dto.RegisterRequest;
import com.shoping.module.auth.service.AuthService;
import com.shoping.module.auth.vo.LoginVO;
import com.shoping.module.user.entity.User;
import com.shoping.module.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final TokenBlacklist tokenBlacklist;

    @Override
    public LoginVO login(LoginRequest request) {
        String account = request.getUsername().trim();
        String ip = currentIp();
        // 限流：锁定中直接拒绝
        if (rateLimiter.isLocked(account, ip)) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED.getCode(),
                    "登录失败次数过多，请 " + rateLimiter.remainLockSeconds(account, ip) + " 秒后重试");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, account).or().eq(User::getPhone, account))
                .last("limit 1"));
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            rateLimiter.recordFailure(account, ip);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        rateLimiter.clear(account, ip);
        Integer role = user.getRole() == null ? 0 : user.getRole();
        String loginName = user.getUsername() != null ? user.getUsername() : user.getPhone();
        return toVO(user, jwtUtil.generate(user.getId(), loginName, role),
                jwtUtil.generateRefresh(user.getId(), loginName, role));
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        String phone = request.getPhone().trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_INVALID);
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }
        User user = new User();
        user.setPhone(phone);
        user.setUsername(phone); // 顾客用户名默认同手机号，便于统一登录
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? "会员" + phone.substring(phone.length() - 4) : request.getNickname().trim());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册成功：phone={}, id={}", phone, user.getId());
        Integer role = user.getRole() == null ? 0 : user.getRole();
        return toVO(user, jwtUtil.generate(user.getId(), user.getUsername(), role),
                jwtUtil.generateRefresh(user.getId(), user.getUsername(), role));
    }

    @Override
    public LoginVO refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parse(request.getRefreshToken());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 必须是 refresh 类型
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 已登出拉黑则拒绝
        if (tokenBlacklist.contains(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Integer role = user.getRole() == null ? 0 : user.getRole();
        String loginName = user.getUsername() != null ? user.getUsername() : user.getPhone();
        return toVO(user, jwtUtil.generate(user.getId(), loginName, role),
                jwtUtil.generateRefresh(user.getId(), loginName, role));
    }

    @Override
    public LoginVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user, null, null);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    private LoginVO toVO(User user, String token, String refreshToken) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // 无请求上下文时退化为固定标识
        }
        return "local";
    }
}
