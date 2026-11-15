package com.shoping.module.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    /** 下单用户 ID（由服务端从登录令牌填充，前端无需传） */
    private Long userId;

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    /** 订单类型：1 标品，2 现制 */
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    /** 备注（少冰/不要吸管等） */
    private String remark;

    /** 使用的用户优惠券 ID（user_coupon.id），可为空 */
    private Long userCouponId;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemRequest> items;
}
