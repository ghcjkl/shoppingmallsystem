package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 商品销量排行 */
@Data
public class ProductSalesVO {

    private String name;

    private Long qty;

    private BigDecimal amount;
}
