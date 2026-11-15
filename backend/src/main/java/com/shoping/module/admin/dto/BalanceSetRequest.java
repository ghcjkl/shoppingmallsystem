package com.shoping.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员直接把储值金额改成指定值
 */
@Data
public class BalanceSetRequest {

    @NotNull(message = "本金余额不能为空")
    @DecimalMin(value = "0", message = "本金余额不能为负")
    private BigDecimal principal;

    @NotNull(message = "赠送余额不能为空")
    @DecimalMin(value = "0", message = "赠送余额不能为负")
    private BigDecimal gift;

    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;
}
