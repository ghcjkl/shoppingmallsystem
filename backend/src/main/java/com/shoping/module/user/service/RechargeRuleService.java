package com.shoping.module.user.service;

import com.shoping.module.admin.dto.RechargeRuleRequest;
import com.shoping.module.user.entity.RechargeRule;
import com.shoping.module.user.vo.RechargeRuleVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值档位规则：赠送金额由服务端根据档位决定
 */
public interface RechargeRuleService {

    /** 前台可见的启用档位（按 sort、金额升序） */
    List<RechargeRuleVO> listEnabled();

    /** 管理端全部档位（含停用） */
    List<RechargeRuleVO> listAll();

    RechargeRuleVO create(RechargeRuleRequest request);

    RechargeRuleVO update(Long id, RechargeRuleRequest request);

    void updateStatus(Long id, Integer status);

    void delete(Long id);

    /** 是否存在启用档位；无档位时允许自定义金额（无赠送） */
    boolean hasEnabledRule();

    /** 按充值金额精确命中启用档位，未命中返回 null */
    RechargeRule matchEnabled(BigDecimal amount);
}
