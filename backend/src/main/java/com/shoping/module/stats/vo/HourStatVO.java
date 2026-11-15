package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 按时段（0-23 时）汇总的销售点 */
@Data
public class HourStatVO {

    /** 小时，0-23 */
    private Integer statHour;

    private Long orders;

    private BigDecimal sales;
}
