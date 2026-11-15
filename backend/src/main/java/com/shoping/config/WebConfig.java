package com.shoping.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置：管理员端 / 用户端两套鉴权拦截器 + 图片静态映射 + 密码加密器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final UserAuthInterceptor userAuthInterceptor;
    private final TokenBlacklist tokenBlacklist;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端：/api/admin/** 全部需要管理员令牌
        registry.addInterceptor(new AdminAuthInterceptor(jwtUtil, tokenBlacklist, false))
                .addPathPatterns("/api/admin/**")
                .order(1);

        // 管理端：查询公开、写操作需要管理员的混合路径
        registry.addInterceptor(new AdminAuthInterceptor(jwtUtil, tokenBlacklist, true))
                .addPathPatterns(
                        "/api/category/**",
                        "/api/material/**",
                        "/api/store",
                        "/api/order/queue/**",
                        "/api/order/*/making",
                        "/api/order/*/ready",
                        "/api/order/*/complete",
                        "/api/order/pickup/verify",
                        "/api/after-sale/process")
                .order(2);

        // 用户端：必须登录（管理员令牌同样可用）
        // 注意：/api/coupon/templates 公开（领券中心展示），其余券/用户/订单/售后需登录
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns(
                        "/api/auth/me",
                        "/api/auth/password",
                        "/api/auth/logout",
                        "/api/user/**",
                        "/api/balance/**",
                        "/api/coupon/receive",
                        "/api/coupon/user/**",
                        "/api/order/**",
                        "/api/after-sale/**",
                        // 评价：提交与查自己订单的评价需登录；商品评价列表（/api/review/product/**）公开
                        "/api/review",
                        "/api/review/order/**",
                        // 站内消息（取餐提醒红点）
                        "/api/notice/**")
                .order(3);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
