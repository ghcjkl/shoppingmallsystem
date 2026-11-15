package com.shoping.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后工单表
 */
@Data
@TableName("after_sale")
public class AfterSale implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 订单明细 ID */
    private Long orderItemId;

    /** 申请前的订单状态（拒绝或售后结束后据此恢复，避免订单卡在「售后中」） */
    private Integer prevOrderStatus;

    /** 用户 ID */
    private Long userId;

    /** 类型：1 退款，2 退货退款，3 补发，4 补偿 */
    private Integer type;

    /** 原因 */
    private String reason;

    /** 凭证图片（JSON 数组） */
    private String imageUrls;

    /** 状态：0 待处理，1 处理中，2 已完成，3 已拒绝 */
    private Integer status;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 处理结果 */
    private String result;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
