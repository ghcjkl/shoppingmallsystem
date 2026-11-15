package com.shoping.module.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存调整请求
 */
@Data
public class StockAdjustRequest {

    /** 1 入库 / 2 出库 / 3 直接设定 */
    @NotNull(message = "请选择调整方式")
    @Min(value = 1, message = "调整方式取值 1-3")
    @Max(value = 3, message = "调整方式取值 1-3")
    private Integer changeType;

    /** 入库/出库为数量（>0），设定为目标库存（>=0） */
    @NotNull(message = "请填写数量")
    @DecimalMin(value = "0", message = "数量不能为负数")
    private BigDecimal quantity;

    private String reason;
}
