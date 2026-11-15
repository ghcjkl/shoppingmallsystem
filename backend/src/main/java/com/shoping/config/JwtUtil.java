package com.shoping.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌工具：生成与解析登录令牌
 * <p>
 * 采用「访问令牌 + 刷新令牌」双令牌机制：
 * <ul>
 *   <li>access token：短时效（默认 2 小时），携带身份，用于访问接口；</li>
 *   <li>refresh token：长时效（默认 14 天），仅用于换取新 access token。</li>
 * </ul>
 * 每个令牌带唯一 jti，供登出黑名单按 jti 精确失效。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:shoping-mall-jwt-secret-key-please-change-me-0123456789}")
    private String secret;

    @Value("${jwt.expire-hours:2}")
    private long expireHours;

    @Value("${jwt.refresh-expire-days:14}")
    private long refreshExpireDays;

    /**
     * 生成访问令牌
     */
    public String generate(Long userId, String username, Integer role) {
        return buildToken(userId, username, role, "access",
                expireHours * 3600_000L);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefresh(Long userId, String username, Integer role) {
        return buildToken(userId, username, role, "refresh",
                refreshExpireDays * 24L * 3600_000L);
    }

    private String buildToken(Long userId, String username, Integer role, String type, long ttlMillis) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌，非法/过期会抛出异常
     */
    public Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 令牌类型：access / refresh
     */
    public String type(String token) {
        Claims claims = parse(token);
        return claims.get("type", String.class);
    }
}
