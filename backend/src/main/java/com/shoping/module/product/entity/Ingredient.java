package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加料表（珍珠/椰果/芝士奶盖…）
 */
@Data
@TableName("ingredient")
public class Ingredient implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 加料名 */
    private String name;

    /** 加料价 */
    private BigDecimal price;

    /** 对应原料 ID（用于扣减原料库存） */
    private Long materialId;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
