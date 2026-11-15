package com.shoping.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.user.dto.RechargeRequest;
import com.shoping.module.user.entity.BalanceAccount;
import com.shoping.module.user.entity.BalanceTransaction;
import com.shoping.module.user.entity.RechargeRule;
import com.shoping.module.user.mapper.BalanceAccountMapper;
import com.shoping.module.user.mapper.BalanceTransactionMapper;
import com.shoping.module.user.service.BalanceService;
import com.shoping.module.user.service.MemberService;
import com.shoping.module.user.service.RechargeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final BalanceAccountMapper balanceAccountMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;
    private final MemberService memberService;
    private final RechargeRuleService rechargeRuleService;

    @Override
    public BalanceAccount getAccount(Long userId) {
        return getOrCreateAccount(userId);
    }

    @Override
    @Transactional
    public BalanceAccount recharge(RechargeRequest request) {
        BalanceAccount account = getOrCreateAccount(request.getUserId());
        BigDecimal amount = request.getAmount();
        // 赠送额只认服务端档位：传了就按档位校验，绝不接受客户端金额
        BigDecimal gift = resolveGift(amount);
        account.setPrincipal(account.getPrincipal().add(amount));
        account.setGift(account.getGift().add(gift));
        account.setTotalRecharge(account.getTotalRecharge().add(amount));
        balanceAccountMapper.updateById(account);
        String remark = gift.compareTo(BigDecimal.ZERO) > 0
                ? "充值（充 " + amount.stripTrailingZeros().toPlainString()
                  + " 送 " + gift.stripTrailingZeros().toPlainString() + "）"
                : "充值";
        insertTransaction(account, 1, amount, gift, null, remark);

        // 充值成功：按实充金额累计积分与成长值（赠送额不产生积分）
        memberService.onRecharge(request.getUserId(), amount);

        return account;
    }

    /** 命中启用档位则返回该档位赠送额；无任何档位时允许自定义金额且不赠送 */
    private BigDecimal resolveGift(BigDecimal amount) {
        if (!rechargeRuleService.hasEnabledRule()) {
            return BigDecimal.ZERO;
        }
        RechargeRule rule = rechargeRuleService.matchEnabled(amount);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_INVALID);
        }
        return rule.getGift() == null ? BigDecimal.ZERO : rule.getGift();
    }

    @Override
    @Transactional
    public void consume(Long userId, Long orderId, BigDecimal amount) {
        BalanceAccount account = getOrCreateAccount(userId);
        BigDecimal total = account.getPrincipal().add(account.getGift());
        if (total.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }
        // 先消耗赠送，后消耗本金
        BigDecimal giftCost = account.getGift().min(amount);
        BigDecimal principalCost = amount.subtract(giftCost);
        account.setGift(account.getGift().subtract(giftCost));
        account.setPrincipal(account.getPrincipal().subtract(principalCost));
        balanceAccountMapper.updateById(account);
        insertTransaction(account, 2, principalCost.negate(), giftCost.negate(), orderId, "消费");
    }

    @Override
    @Transactional
    public void refund(Long userId, Long orderId, BigDecimal amount) {
        // 底层兜底：退款金额必须为正，具体额度上限由售后业务层按订单实付校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额必须大于 0");
        }
        BalanceAccount account = getOrCreateAccount(userId);
        account.setPrincipal(account.getPrincipal().add(amount));
        balanceAccountMapper.updateById(account);
        insertTransaction(account, 3, amount, BigDecimal.ZERO, orderId, "退款");
    }

    @Override
    public List<BalanceTransaction> transactions(Long userId) {
        return balanceTransactionMapper.selectList(new LambdaQueryWrapper<BalanceTransaction>()
                .eq(BalanceTransaction::getUserId, userId)
                .orderByDesc(BalanceTransaction::getId));
    }

    @Override
    @Transactional
    public BalanceAccount adjust(Long userId, BigDecimal principalDelta, BigDecimal giftDelta, String remark) {
        BalanceAccount account = getOrCreateAccount(userId);
        BigDecimal principal = nvl(account.getPrincipal()).add(nvl(principalDelta));
        BigDecimal gift = nvl(account.getGift()).add(nvl(giftDelta));
        if (principal.compareTo(BigDecimal.ZERO) < 0 || gift.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BALANCE_AMOUNT_INVALID);
        }
        account.setPrincipal(principal);
        account.setGift(gift);
        balanceAccountMapper.updateById(account);
        insertTransaction(account, 5, nvl(principalDelta), nvl(giftDelta), null,
                remark == null || remark.isBlank() ? "管理员调整" : remark);
        return account;
    }

    @Override
    @Transactional
    public BalanceAccount setAmount(Long userId, BigDecimal principal, BigDecimal gift, String remark) {
        BalanceAccount account = getOrCreateAccount(userId);
        BigDecimal newPrincipal = nvl(principal);
        BigDecimal newGift = nvl(gift);
        if (newPrincipal.compareTo(BigDecimal.ZERO) < 0 || newGift.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BALANCE_AMOUNT_INVALID);
        }
        BigDecimal principalDelta = newPrincipal.subtract(nvl(account.getPrincipal()));
        BigDecimal giftDelta = newGift.subtract(nvl(account.getGift()));
        account.setPrincipal(newPrincipal);
        account.setGift(newGift);
        balanceAccountMapper.updateById(account);
        insertTransaction(account, 5, principalDelta, giftDelta, null,
                remark == null || remark.isBlank() ? "管理员设为指定金额" : remark);
        return account;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BalanceAccount getOrCreateAccount(Long userId) {
        BalanceAccount account = balanceAccountMapper.selectOne(new LambdaQueryWrapper<BalanceAccount>()
                .eq(BalanceAccount::getUserId, userId));
        if (account == null) {
            account = new BalanceAccount();
            account.setUserId(userId);
            account.setPrincipal(BigDecimal.ZERO);
            account.setGift(BigDecimal.ZERO);
            account.setTotalRecharge(BigDecimal.ZERO);
            balanceAccountMapper.insert(account);
        }
        return account;
    }

    private void insertTransaction(BalanceAccount account, int type, BigDecimal principalAmount,
                                    BigDecimal giftAmount, Long orderId, String remark) {
        BalanceTransaction tx = new BalanceTransaction();
        tx.setUserId(account.getUserId());
        tx.setType(type);
        tx.setPrincipalAmount(principalAmount);
        tx.setGiftAmount(giftAmount);
        tx.setBalancePrincipal(account.getPrincipal());
        tx.setBalanceGift(account.getGift());
        tx.setOrderId(orderId);
        tx.setRemark(remark);
        balanceTransactionMapper.insert(tx);
    }
}
