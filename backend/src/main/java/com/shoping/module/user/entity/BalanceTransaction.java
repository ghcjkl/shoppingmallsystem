package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值流水表（充值/消费/退款/赠送/人工调整，全程可查）
 */
@Data
@TableName("balance_transaction")
public class BalanceTransaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 类型：1 充值，2 消费，3 退款，4 赠送，5 人工调整 */
    private Integer type;

    /** 本金变动（正增负减） */
    private BigDecimal principalAmount;

    /** 赠送变动（正增负减） */
    private BigDecimal giftAmount;

    /** 变动后本金余额 */
    private BigDecimal balancePrincipal;

    /** 变动后赠送余额 */
    private BigDecimal balanceGift;

    /** 关联订单 */
    private Long orderId;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
}
