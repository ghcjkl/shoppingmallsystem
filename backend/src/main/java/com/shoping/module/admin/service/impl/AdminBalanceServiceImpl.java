package com.shoping.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.admin.service.AdminBalanceService;
import com.shoping.module.admin.vo.UserBalanceVO;
import com.shoping.module.user.entity.BalanceAccount;
import com.shoping.module.user.entity.User;
import com.shoping.module.user.mapper.BalanceAccountMapper;
import com.shoping.module.user.mapper.UserMapper;
import com.shoping.module.user.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBalanceServiceImpl implements AdminBalanceService {

    private final UserMapper userMapper;
    private final BalanceAccountMapper balanceAccountMapper;
    private final BalanceService balanceService;

    @Override
    public List<UserBalanceVO> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId)
                .last("limit 20");
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getPhone, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getUsername, kw));
        }
        List<User> users = userMapper.selectList(wrapper);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<BalanceAccount> accounts = balanceAccountMapper.selectList(
                new LambdaQueryWrapper<BalanceAccount>().in(BalanceAccount::getUserId, userIds));
        var accountMap = accounts.stream()
                .collect(Collectors.toMap(BalanceAccount::getUserId, Function.identity(), (a, b) -> a));
        return users.stream().map(u -> toVO(u, accountMap.get(u.getId()))).collect(Collectors.toList());
    }

    @Override
    public UserBalanceVO getAccount(Long userId) {
        User user = getUser(userId);
        BalanceAccount account = balanceAccountMapper.selectOne(
                new LambdaQueryWrapper<BalanceAccount>().eq(BalanceAccount::getUserId, userId));
        return toVO(user, account);
    }

    @Override
    public UserBalanceVO adjust(Long userId, BigDecimal principalDelta, BigDecimal giftDelta, String remark) {
        getUser(userId);
        BalanceAccount account = balanceService.adjust(userId, principalDelta, giftDelta, remark);
        return toVO(getUser(userId), account);
    }

    @Override
    public UserBalanceVO setAmount(Long userId, BigDecimal principal, BigDecimal gift, String remark) {
        getUser(userId);
        BalanceAccount account = balanceService.setAmount(userId, principal, gift, remark);
        return toVO(getUser(userId), account);
    }

    private User getUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserBalanceVO toVO(User user, BalanceAccount account) {
        UserBalanceVO vo = new UserBalanceVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setUsername(user.getUsername());
        vo.setStatus(user.getStatus());
        BigDecimal principal = account == null ? BigDecimal.ZERO : nvl(account.getPrincipal());
        BigDecimal gift = account == null ? BigDecimal.ZERO : nvl(account.getGift());
        vo.setPrincipal(principal);
        vo.setGift(gift);
        vo.setTotal(principal.add(gift));
        vo.setTotalRecharge(account == null ? BigDecimal.ZERO : nvl(account.getTotalRecharge()));
        return vo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
