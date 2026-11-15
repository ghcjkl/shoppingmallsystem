package com.shoping.module.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值请求：只接受金额，赠送金额由服务端按充值档位计算，客户端无法指定
 */
@Data
public class RechargeRequest {

    /** 用户 ID（由服务端从登录令牌填充，前端无需传） */
    private Long userId;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;
}
