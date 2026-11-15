package com.shoping.module.admin.controller;

import com.shoping.common.result.Result;
import com.shoping.module.admin.dto.RechargeRuleRequest;
import com.shoping.module.user.service.RechargeRuleService;
import com.shoping.module.user.vo.RechargeRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 管理端 - 充值档位（充 N 送 M）配置
 */
@RestController
@RequestMapping("/api/admin/recharge-rule")
@RequiredArgsConstructor
public class AdminRechargeRuleController {

    private final RechargeRuleService rechargeRuleService;

    @GetMapping
    public Result<List<RechargeRuleVO>> list() {
        return Result.ok(rechargeRuleService.listAll());
    }

    @PostMapping
    public Result<RechargeRuleVO> create(@Valid @RequestBody RechargeRuleRequest request) {
        return Result.ok(rechargeRuleService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RechargeRuleVO> update(@PathVariable Long id,
                                         @Valid @RequestBody RechargeRuleRequest request) {
        return Result.ok(rechargeRuleService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        rechargeRuleService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rechargeRuleService.delete(id);
        return Result.ok();
    }
}
