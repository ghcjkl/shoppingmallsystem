package com.shoping.module.stock.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品库存行（按 SKU 展开）
 */
@Data
public class SkuStockVO {

    private Long skuId;
    private Long productId;
    private String productName;
    /** 1 标品 2 现制 */
    private Integer productType;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    /** SKU 状态：1 启用 0 停用 */
    private Integer skuStatus;
    /** 商品状态：1 上架 0 下架 */
    private Integer productStatus;
}
