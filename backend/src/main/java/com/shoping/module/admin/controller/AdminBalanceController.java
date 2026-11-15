package com.shoping.module.admin.controller;

import com.shoping.common.result.Result;
import com.shoping.module.admin.dto.BalanceAdjustRequest;
import com.shoping.module.admin.dto.BalanceSetRequest;
import com.shoping.module.admin.service.AdminBalanceService;
import com.shoping.module.admin.vo.UserBalanceVO;
import com.shoping.module.user.entity.BalanceTransaction;
import com.shoping.module.user.service.BalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 储值账户管理
 */
@RestController
@RequestMapping("/api/admin/balance")
@RequiredArgsConstructor
public class AdminBalanceController {

    private final AdminBalanceService adminBalanceService;
    private final BalanceService balanceService;

    /** 搜索用户及其储值余额（手机号 / 昵称 / 用户名） */
    @GetMapping("/users")
    public Result<List<UserBalanceVO>> users(@RequestParam(required = false) String keyword) {
        return Result.ok(adminBalanceService.searchUsers(keyword));
    }

    @GetMapping("/{userId}")
    public Result<UserBalanceVO> account(@PathVariable Long userId) {
        return Result.ok(adminBalanceService.getAccount(userId));
    }

    /** 储值流水（含人工调整记录） */
    @GetMapping("/{userId}/transactions")
    public Result<List<BalanceTransaction>> transactions(@PathVariable Long userId) {
        return Result.ok(balanceService.transactions(userId));
    }

    /** 增减金额：正数增加、负数减少，写 type=5 人工调整流水 */
    @PostMapping("/{userId}/adjust")
    public Result<UserBalanceVO> adjust(@PathVariable Long userId,
                                        @Valid @RequestBody BalanceAdjustRequest request) {
        return Result.ok(adminBalanceService.adjust(userId,
                request.getPrincipalDelta(), request.getGiftDelta(), request.getRemark()));
    }

    /** 直接把金额改为指定值，按差额写 type=5 人工调整流水 */
    @PutMapping("/{userId}")
    public Result<UserBalanceVO> setAmount(@PathVariable Long userId,
                                           @Valid @RequestBody BalanceSetRequest request) {
        return Result.ok(adminBalanceService.setAmount(userId,
                request.getPrincipal(), request.getGift(), request.getRemark()));
    }
}
