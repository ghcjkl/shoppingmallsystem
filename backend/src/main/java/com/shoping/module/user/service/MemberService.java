package com.shoping.module.user.service;

import com.shoping.module.user.entity.Member;
import com.shoping.module.user.entity.MemberPointRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员积分/等级服务
 */
public interface MemberService {

    /** 获取或创建会员账户 */
    Member getOrCreate(Long userId);

    /** 消费奖励：按实付金额累计积分 + 成长值（下单支付成功后调用） */
    void onPaid(Long userId, Long orderId, BigDecimal payAmount);

    /** 充值奖励：按充值金额累计积分 + 成长值（充值成功后调用） */
    void onRecharge(Long userId, BigDecimal amount);

    /**
     * 退款扣回：按退款金额扣减积分与成长值，并重算等级（售后完成退款时调用）
     * <p>不会扣成负数：账户余额不足时扣到 0 为止。</p>
     */
    void onRefund(Long userId, Long orderId, BigDecimal refundAmount);

    /** 积分流水明细 */
    List<MemberPointRecord> pointRecords(Long userId);

    /**
     * 积分抵扣下单：原子扣减积分并写 type=3 负数流水。
     * @return 是否扣减成功（false 表示积分不足）
     */
    boolean usePoints(Long userId, Long orderId, int points);

    /**
     * 积分抵扣回退（订单取消 / 售后退款时调用）：原子回加积分并写 type=6 正数流水。
     */
    void refundPoints(Long userId, Long orderId, int points);
}
