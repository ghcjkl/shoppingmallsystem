package com.shoping.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long couponId;
    private String name;
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private Integer scope;
    private Integer status;
    private LocalDateTime endTime;
}
