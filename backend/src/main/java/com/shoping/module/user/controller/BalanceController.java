package com.shoping.module.user.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.module.user.dto.RechargeRequest;
import com.shoping.module.user.entity.BalanceAccount;
import com.shoping.module.user.entity.BalanceTransaction;
import com.shoping.module.user.service.BalanceService;
import com.shoping.module.user.service.RechargeRuleService;
import com.shoping.module.user.vo.RechargeRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final RechargeRuleService rechargeRuleService;

    @GetMapping("/{userId}")
    public Result<BalanceAccount> get(@PathVariable Long userId) {
        return Result.ok(balanceService.getAccount(userId));
    }

    /** 充值档位：前端只展示「充 N 到账 N+M」，赠送额由服务端决定 */
    @GetMapping("/rules")
    public Result<List<RechargeRuleVO>> rules() {
        return Result.ok(rechargeRuleService.listEnabled());
    }

    @PostMapping("/recharge")
    public Result<BalanceAccount> recharge(@Valid @RequestBody RechargeRequest request) {
        request.setUserId(AuthContext.requireUserId());
        return Result.ok(balanceService.recharge(request));
    }

    @GetMapping("/{userId}/transactions")
    public Result<List<BalanceTransaction>> transactions(@PathVariable Long userId) {
        return Result.ok(balanceService.transactions(userId));
    }
}
