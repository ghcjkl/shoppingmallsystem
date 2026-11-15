package com.shoping.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端新增/编辑充值档位
 */
@Data
public class RechargeRuleRequest {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "赠送金额不能为空")
    @DecimalMin(value = "0", message = "赠送金额不能为负")
    private BigDecimal gift;

    /** 排序，可不传 */
    private Integer sort;

    /** 状态：1 启用，0 停用，默认启用 */
    private Integer status;
}
