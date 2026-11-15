package com.shoping.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员增减储值金额（正数增加、负数减少）
 */
@Data
public class BalanceAdjustRequest {

    @NotNull(message = "本金变动不能为空")
    private BigDecimal principalDelta;

    @NotNull(message = "赠送变动不能为空")
    private BigDecimal giftDelta;

    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;
}
