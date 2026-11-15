package com.shoping.module.aftersale.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleProcessRequest {

    private Long id;

    /** 状态：0 待处理，1 处理中，2 已完成，3 已拒绝 */
    private Integer status;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 处理结果 */
    private String result;

    /**
     * 是否回滚库存（仅「退款 / 退货退款」有效，默认 true；补发、补偿忽略此字段）
     * 商品已损毁/已被顾客丢弃时可传 false
     */
    private Boolean restock;
}
