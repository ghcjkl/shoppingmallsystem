package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 单日销售趋势点 */
@Data
public class TrendPointVO {

    private LocalDate statDate;

    private Long orders;

    private BigDecimal sales;
}
