package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 订单类型（标品/现制）汇总 */
@Data
public class OrderTypeStatVO {

    /** 1 标品 2 现制 */
    private Integer orderType;

    private Long orders;

    private BigDecimal amount;
}
