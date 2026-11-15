package com.shoping.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端 SKU 维护项
 */
@Data
public class AdminSkuRequest {

    /** 已存在的 SKU 传 id，新增留空 */
    private Long id;

    /** 规格名，如 中杯 / 默认 */
    private String skuCode;

    @DecimalMin(value = "0", message = "售价不能为负")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    /** 状态：1 可用，0 禁用 */
    private Integer status;
}
