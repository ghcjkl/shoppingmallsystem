package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值账户表（本金与赠送分账，退款时需区分）
 */
@Data
@TableName("balance_account")
public class BalanceAccount implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 本金余额 */
    private BigDecimal principal;

    /** 赠送余额 */
    private BigDecimal gift;

    /** 累计充值 */
    private BigDecimal totalRecharge;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
