package com.shoping.module.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 支付方式：1 线上，2 储值，3 混合 */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
