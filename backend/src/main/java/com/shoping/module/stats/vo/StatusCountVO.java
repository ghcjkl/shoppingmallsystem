package com.shoping.module.stats.vo;

import lombok.Data;

/** 订单状态分布 */
@Data
public class StatusCountVO {

    private Integer status;

    private Long orders;
}
