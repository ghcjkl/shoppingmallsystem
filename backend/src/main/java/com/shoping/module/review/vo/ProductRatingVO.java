package com.shoping.module.review.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 商品评分汇总（列表页批量填充用） */
@Data
public class ProductRatingVO {

    private Long productId;

    /** 平均分，1 位小数 */
    private BigDecimal rating;

    private Long reviewCount;
}
