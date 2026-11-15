package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规格组表（杯型/温度/甜度/冰量）
 */
@Data
@TableName("spec_group")
public class SpecGroup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规格组名 */
    private String name;

    /** 商品类型：1 标品，2 现制，NULL 通用 */
    private Integer productType;

    /** 排序 */
    private Integer sort;

    private LocalDateTime createTime;
}
