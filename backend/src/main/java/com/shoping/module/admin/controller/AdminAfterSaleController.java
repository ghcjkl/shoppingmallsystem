package com.shoping.module.admin.controller;

import com.shoping.common.result.Result;
import com.shoping.module.aftersale.dto.AfterSaleProcessRequest;
import com.shoping.module.aftersale.entity.AfterSale;
import com.shoping.module.aftersale.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端售后处理：查看所有售后单并处理（同意退款/拒绝等）
 */
@RestController
@RequestMapping("/api/admin/after-sale")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    /**
     * 售后单列表（可按状态筛选，status 为空查全部）
     */
    @GetMapping("/list")
    public Result<List<AfterSale>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(afterSaleService.listAll(status));
    }

    /**
     * 处理售后（同意/拒绝/退款）
     */
    @PostMapping("/process")
    public Result<AfterSale> process(@RequestBody AfterSaleProcessRequest request) {
        return Result.ok(afterSaleService.process(request));
    }
}
