package com.shoping.module.aftersale.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.module.aftersale.dto.AfterSaleApplyRequest;
import com.shoping.module.aftersale.dto.AfterSaleProcessRequest;
import com.shoping.module.aftersale.entity.AfterSale;
import com.shoping.module.aftersale.service.AfterSaleService;
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
@RequestMapping("/api/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @PostMapping("/apply")
    public Result<AfterSale> apply(@Valid @RequestBody AfterSaleApplyRequest request) {
        request.setUserId(AuthContext.requireUserId());
        return Result.ok(afterSaleService.apply(request));
    }

    @GetMapping("/user/{userId}")
    public Result<List<AfterSale>> listByUser(@PathVariable Long userId) {
        return Result.ok(afterSaleService.listByUser(userId));
    }

    @PostMapping("/process")
    public Result<AfterSale> process(@RequestBody AfterSaleProcessRequest request) {
        return Result.ok(afterSaleService.process(request));
    }
}
