package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 时间段汇总（销售额 / 订单数 / 客单价） */
@Data
public class PeriodStatVO {

    private BigDecimal sales;

    private Long orders;
}
