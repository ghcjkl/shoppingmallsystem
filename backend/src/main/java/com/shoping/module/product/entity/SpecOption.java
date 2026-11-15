package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 规格选项表（中杯/大杯、冰/常温/热…）
 */
@Data
@TableName("spec_option")
public class SpecOption implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规格组 ID */
    private Long groupId;

    /** 选项名 */
    private String name;

    /** 加价金额 */
    private BigDecimal priceDelta;

    /** 排序 */
    private Integer sort;

    private LocalDateTime createTime;
}
