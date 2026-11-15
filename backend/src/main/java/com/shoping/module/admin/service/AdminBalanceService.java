package com.shoping.module.admin.service;

import com.shoping.module.admin.vo.UserBalanceVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 - 储值账户查询与金额调整
 */
public interface AdminBalanceService {

    /** 按手机号 / 昵称 / 用户名搜索用户（最多 20 条），keyword 为空返回最近用户 */
    List<UserBalanceVO> searchUsers(String keyword);

    UserBalanceVO getAccount(Long userId);

    /** 增减金额（正数增加、负数减少） */
    UserBalanceVO adjust(Long userId, BigDecimal principalDelta, BigDecimal giftDelta, String remark);

    /** 直接把金额改成指定值 */
    UserBalanceVO setAmount(Long userId, BigDecimal principal, BigDecimal gift, String remark);
}
