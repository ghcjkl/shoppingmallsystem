package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * SKU 与规格选项关联表（决定一个 SKU 是"中杯/冰/半糖"组合）
 */
@Data
@TableName("sku_spec_option")
public class SkuSpecOption implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** 规格选项 ID */
    private Long specOptionId;
}
