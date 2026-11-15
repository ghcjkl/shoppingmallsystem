package com.shoping.module.user.vo;

import com.shoping.module.user.entity.RechargeRule;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值档位展示对象：前端只展示「充 ¥amount 到账 ¥total」
 */
@Data
public class RechargeRuleVO {

    private Long id;

    /** 充值金额 */
    private BigDecimal amount;

    /** 赠送金额（服务端计算，仅供展示） */
    private BigDecimal gift;

    /** 到账金额 = 充值 + 赠送 */
    private BigDecimal total;

    private Integer sort;

    private Integer status;

    public static RechargeRuleVO of(RechargeRule rule) {
        RechargeRuleVO vo = new RechargeRuleVO();
        vo.setId(rule.getId());
        BigDecimal amount = rule.getAmount() == null ? BigDecimal.ZERO : rule.getAmount();
        BigDecimal gift = rule.getGift() == null ? BigDecimal.ZERO : rule.getGift();
        vo.setAmount(amount);
        vo.setGift(gift);
        vo.setTotal(amount.add(gift));
        vo.setSort(rule.getSort());
        vo.setStatus(rule.getStatus());
        return vo;
    }
}
