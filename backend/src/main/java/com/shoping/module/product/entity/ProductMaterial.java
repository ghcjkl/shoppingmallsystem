package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品原料 BOM 表（原料售罄 -> 关联商品自动下架的核心依赖）
 */
@Data
@TableName("product_material")
public class ProductMaterial implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** SKU ID，NULL 表示商品级（所有 SKU） */
    private Long skuId;

    /** 原料 ID */
    private Long materialId;

    /** 每份用量 */
    private BigDecimal quantity;
}
