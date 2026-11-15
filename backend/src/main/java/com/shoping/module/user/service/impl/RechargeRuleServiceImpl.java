package com.shoping.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.admin.dto.RechargeRuleRequest;
import com.shoping.module.user.entity.RechargeRule;
import com.shoping.module.user.mapper.RechargeRuleMapper;
import com.shoping.module.user.service.RechargeRuleService;
import com.shoping.module.user.vo.RechargeRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RechargeRuleServiceImpl implements RechargeRuleService {

    private final RechargeRuleMapper rechargeRuleMapper;
    private final CacheService cacheService;

    @Override
    public List<RechargeRuleVO> listEnabled() {
        return list(true);
    }

    @Override
    public List<RechargeRuleVO> listAll() {
        return list(false);
    }

    /** 档位是纯配置数据，顾客端与管理端都读，缓存 5 分钟；任何写操作立即清掉两份 */
    private List<RechargeRuleVO> list(boolean onlyEnabled) {
        String key = CacheKeys.rechargeRules(onlyEnabled);
        return cacheService.remember(key, CacheKeys.TTL_RECHARGE_RULES,
                new TypeReference<List<RechargeRuleVO>>() {
                }, () -> query(onlyEnabled));
    }

    private List<RechargeRuleVO> query(boolean onlyEnabled) {
        return rechargeRuleMapper.selectList(new LambdaQueryWrapper<RechargeRule>()
                        .eq(onlyEnabled, RechargeRule::getStatus, 1)
                        .orderByAsc(RechargeRule::getSort)
                        .orderByAsc(RechargeRule::getAmount))
                .stream().map(RechargeRuleVO::of).collect(Collectors.toList());
    }

    /** 档位变更后清掉前台与后台两份列表缓存 */
    private void evictLists() {
        cacheService.evict(CacheKeys.rechargeRules(true), CacheKeys.rechargeRules(false));
    }

    @Override
    @Transactional
    public RechargeRuleVO create(RechargeRuleRequest request) {
        BigDecimal amount = normalize(request.getAmount());
        ensureAmountUnique(amount, null);
        RechargeRule rule = new RechargeRule();
        rule.setAmount(amount);
        rule.setGift(normalize(request.getGift()));
        rule.setSort(request.getSort() == null ? 0 : request.getSort());
        rule.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        rechargeRuleMapper.insert(rule);
        evictLists();
        return RechargeRuleVO.of(rechargeRuleMapper.selectById(rule.getId()));
    }

    @Override
    @Transactional
    public RechargeRuleVO update(Long id, RechargeRuleRequest request) {
        RechargeRule rule = requireRule(id);
        BigDecimal amount = normalize(request.getAmount());
        ensureAmountUnique(amount, id);
        rule.setAmount(amount);
        rule.setGift(normalize(request.getGift()));
        if (request.getSort() != null) {
            rule.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            rule.setStatus(request.getStatus());
        }
        rechargeRuleMapper.updateById(rule);
        evictLists();
        return RechargeRuleVO.of(rechargeRuleMapper.selectById(id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        RechargeRule rule = requireRule(id);
        rule.setStatus(status);
        rechargeRuleMapper.updateById(rule);
        evictLists();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRule(id);
        rechargeRuleMapper.deleteById(id);
        evictLists();
    }

    @Override
    public boolean hasEnabledRule() {
        return rechargeRuleMapper.selectCount(new LambdaQueryWrapper<RechargeRule>()
                .eq(RechargeRule::getStatus, 1)) > 0;
    }

    @Override
    public RechargeRule matchEnabled(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return rechargeRuleMapper.selectList(new LambdaQueryWrapper<RechargeRule>()
                        .eq(RechargeRule::getStatus, 1))
                .stream()
                .filter(r -> r.getAmount() != null && r.getAmount().compareTo(normalize(amount)) == 0)
                .findFirst()
                .orElse(null);
    }

    private RechargeRule requireRule(Long id) {
        RechargeRule rule = rechargeRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RECHARGE_RULE_NOT_FOUND);
        }
        return rule;
    }

    /** 同一充值金额只允许一条档位，避免赠送额出现歧义 */
    private void ensureAmountUnique(BigDecimal amount, Long excludeId) {
        List<RechargeRule> exists = rechargeRuleMapper.selectList(new LambdaQueryWrapper<RechargeRule>()
                        .eq(RechargeRule::getAmount, amount))
                .stream()
                .filter(r -> excludeId == null || !excludeId.equals(r.getId()))
                .collect(Collectors.toList());
        if (!exists.isEmpty()) {
            throw new BusinessException(ErrorCode.RECHARGE_RULE_EXISTS);
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
