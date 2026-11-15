package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 原料表（门店原料库存，支付成功后按 BOM 扣减）
 */
@Data
@TableName("material")
public class Material implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原料名 */
    private String name;

    /** 单位 */
    private String unit;

    /** 当前库存 */
    private BigDecimal stock;

    /** 安全库存阈值 */
    private BigDecimal safeStock;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
