package com.shoping.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌黑名单（内存版）：登出后加入，拦截器据此拒绝已失效的令牌。
 * <p>
 * 按 jti 记录，并附带令牌过期时间——过期后自动失效，无需长期驻留，
 * 避免内存无限增长。单机/演示环境足够；多实例部署需替换为 Redis。
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private final JwtUtil jwtUtil;

    /** jti -> 令牌过期时间戳（ms） */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 将令牌加入黑名单（登出时调用）
     */
    public void add(String token) {
        try {
            Claims claims = jwtUtil.parse(token);
            String jti = claims.getId();
            Date exp = claims.getExpiration();
            if (jti != null && exp != null) {
                blacklist.put(jti, exp.getTime());
            }
        } catch (Exception ignored) {
            // 非法令牌无需加入黑名单
        }
        // 顺带清理已过期的记录，防止内存膨胀
        cleanup();
    }

    /**
     * 令牌是否已被拉黑
     */
    public boolean contains(String token) {
        try {
            Claims claims = jwtUtil.parse(token);
            String jti = claims.getId();
            if (jti == null) {
                return false;
            }
            Long exp = blacklist.get(jti);
            return exp != null && exp > System.currentTimeMillis();
        } catch (Exception e) {
            // 解析失败说明令牌本身非法，交由后续校验处理
            return false;
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= now);
    }
}
