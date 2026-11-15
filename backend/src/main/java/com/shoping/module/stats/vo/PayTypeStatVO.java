package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 支付方式占比 */
@Data
public class PayTypeStatVO {

    /** 1 线上 2 储值 3 混合 */
    private Integer payType;

    private Long orders;

    private BigDecimal amount;
}
