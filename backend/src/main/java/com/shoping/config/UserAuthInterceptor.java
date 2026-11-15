package com.shoping.config;

import com.shoping.common.context.AuthContext;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.result.JsonResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 普通用户端鉴权：必须携带有效令牌；并把令牌中的用户身份写入 {@link AuthContext}
 * <p>
 * 若路径里带 {userId}，会校验其与令牌身份一致，避免改个数字就能操作别人的数据。
 */
@Component
@RequiredArgsConstructor
public class UserAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            JsonResponse.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
            return false;
        }
        // 登出拉黑校验
        if (tokenBlacklist.contains(token)) {
            JsonResponse.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED.getCode(), "登录已失效，请重新登录");
            return false;
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            JsonResponse.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
            return false;
        }
        Long userId = Long.valueOf(claims.getSubject());
        Integer role = claims.get("role", Integer.class);
        AuthContext.set(userId, role, claims.get("username", String.class));

        if (role == null || role != AuthContext.ROLE_ADMIN) {
            Long pathUserId = resolvePathUserId(request);
            if (pathUserId != null && !pathUserId.equals(userId)) {
                JsonResponse.write(response, HttpServletResponse.SC_FORBIDDEN,
                        ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    @SuppressWarnings("unchecked")
    private Long resolvePathUserId(HttpServletRequest request) {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map)) {
            return null;
        }
        Object value = ((Map<String, String>) attr).get("userId");
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
