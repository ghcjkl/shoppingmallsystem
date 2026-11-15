package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（标品零食 / 现制饮品共用主表，规格差异由 SKU 承载）
 */
@Data
@TableName("product")
public class Product implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品类型：1 标品零食，2 现制饮品 */
    private Integer productType;

    /** 分类 ID */
    private Long categoryId;

    /** 主图 URL */
    private String coverImg;

    /** 商品详情 */
    private String detail;

    /** 基准价（现制为默认杯型价） */
    private BigDecimal basePrice;

    /** 状态：1 上架，0 下架 */
    private Integer status;

    /** 净含量（标品） */
    private String netContent;

    /** 保质期（标品） */
    private String shelfLife;

    /** 过敏原 */
    private String allergen;

    /** 预计出餐时长/分钟（现制） */
    private Integer estMakeMinutes;

    /** 热量/千卡（现制） */
    private Integer calories;

    /** 糖分提示（现制） */
    private String sugarNote;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
