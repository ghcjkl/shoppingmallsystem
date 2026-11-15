package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 按商品分类汇总的销售结构 */
@Data
public class CategoryStatVO {

    private Long categoryId;

    private String name;

    private Long qty;

    private BigDecimal amount;
}
