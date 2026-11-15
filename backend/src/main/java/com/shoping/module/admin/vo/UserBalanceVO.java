package com.shoping.module.admin.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端：用户 + 储值账户概览
 */
@Data
public class UserBalanceVO implements Serializable {

    private Long userId;
    private String nickname;
    private String phone;
    private String username;

    /** 本金余额 */
    private BigDecimal principal;
    /** 赠送余额 */
    private BigDecimal gift;
    /** 合计可用 */
    private BigDecimal total;
    /** 累计充值 */
    private BigDecimal totalRecharge;

    /** 用户状态：1 正常，0 禁用 */
    private Integer status;
}
