package com.shoping.module.auth.vo;

import lombok.Data;

/**
 * 登录结果（令牌 + 用户信息）
 */
@Data
public class LoginVO {

    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private Integer role;
}
