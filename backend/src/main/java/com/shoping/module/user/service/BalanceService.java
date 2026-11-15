package com.shoping.module.user.service;

import com.shoping.module.user.dto.RechargeRequest;
import com.shoping.module.user.entity.BalanceAccount;
import com.shoping.module.user.entity.BalanceTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface BalanceService {

    BalanceAccount getAccount(Long userId);

    BalanceAccount recharge(RechargeRequest request);

    /** 消费扣减（先消耗赠送、后消耗本金），余额不足抛异常 */
    void consume(Long userId, Long orderId, BigDecimal amount);

    /** 退款：退回本金 */
    void refund(Long userId, Long orderId, BigDecimal amount);

    List<BalanceTransaction> transactions(Long userId);

    /** 管理员增减金额：正数增加、负数减少，变动记 type=5 人工调整流水 */
    BalanceAccount adjust(Long userId, BigDecimal principalDelta, BigDecimal giftDelta, String remark);

    /** 管理员直接设定金额：按差额记 type=5 人工调整流水 */
    BalanceAccount setAmount(Long userId, BigDecimal principal, BigDecimal gift, String remark);
}
