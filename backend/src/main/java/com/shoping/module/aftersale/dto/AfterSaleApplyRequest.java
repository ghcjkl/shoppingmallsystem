package com.shoping.module.aftersale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleApplyRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 订单明细ID，可为空 */
    private Long orderItemId;

    /** 用户 ID（由服务端从登录令牌填充，前端无需传） */
    private Long userId;

    /** 类型：1 退款，2 退货退款，3 补发，4 补偿 */
    @NotNull(message = "售后类型不能为空")
    private Integer type;

    private String reason;

    /** 凭证图片（JSON 数组） */
    private String imageUrls;
}
