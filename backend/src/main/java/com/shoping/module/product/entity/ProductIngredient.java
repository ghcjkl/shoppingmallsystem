package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品可选加料关联表
 */
@Data
@TableName("product_ingredient")
public class ProductIngredient implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 加料 ID */
    private Long ingredientId;

    /** 该商品下加料价（可覆盖默认） */
    private BigDecimal price;
}
