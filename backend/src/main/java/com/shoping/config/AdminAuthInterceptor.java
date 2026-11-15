package com.shoping.config;

import com.shoping.common.context.AuthContext;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.result.JsonResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员端鉴权：校验 Authorization: Bearer <token> 且角色为管理员
 * <p>
 * allowRead = true 时放行 GET/HEAD，用于“读公开、写需管理员”的混合路径
 * （分类、原料、门店、订单核销等）。
 */
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    /** 是否放行读请求（公开的查询接口） */
    private final boolean allowRead;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (allowRead && (HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod()))) {
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
        try {
            Claims claims = jwtUtil.parse(token);
            Integer role = claims.get("role", Integer.class);
            if (role == null || role != AuthContext.ROLE_ADMIN) {
                JsonResponse.write(response, HttpServletResponse.SC_FORBIDDEN,
                        ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
                return false;
            }
            Long userId = Long.valueOf(claims.getSubject());
            AuthContext.set(userId, role, claims.get("username", String.class));
            request.setAttribute("adminUserId", userId);
            return true;
        } catch (Exception e) {
            JsonResponse.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
