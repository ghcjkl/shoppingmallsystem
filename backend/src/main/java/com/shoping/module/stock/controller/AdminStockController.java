package com.shoping.module.stock.controller;

import com.shoping.common.result.PageResult;
import com.shoping.common.result.Result;
import com.shoping.module.product.entity.Material;
import com.shoping.module.stock.dto.StockAdjustRequest;
import com.shoping.module.stock.entity.StockRecord;
import com.shoping.module.stock.service.StockService;
import com.shoping.module.stock.vo.SkuStockVO;
import com.shoping.module.stock.vo.StockSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端库存：商品 SKU 库存、原料库存、库存流水
 */
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;

    /** 商品库存分页（按 SKU） */
    @GetMapping("/sku/page")
    public Result<PageResult<SkuStockVO>> skuPage(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean lowOnly,
                                                  @RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(stockService.skuPage(keyword, lowOnly, pageNum, pageSize));
    }

    /** 调整某个 SKU 的库存 */
    @PostMapping("/sku/{skuId}/adjust")
    public Result<Void> adjustSku(@PathVariable Long skuId, @Valid @RequestBody StockAdjustRequest request) {
        stockService.adjustSku(skuId, request);
        return Result.ok();
    }

    /** 原料库存列表 */
    @GetMapping("/material/list")
    public Result<List<Material>> materials() {
        return Result.ok(stockService.materials());
    }

    /** 调整某个原料的库存 */
    @PostMapping("/material/{materialId}/adjust")
    public Result<Void> adjustMaterial(@PathVariable Long materialId,
                                       @Valid @RequestBody StockAdjustRequest request) {
        stockService.adjustMaterial(materialId, request);
        return Result.ok();
    }

    /** 库存概览 */
    @GetMapping("/summary")
    public Result<StockSummaryVO> summary() {
        return Result.ok(stockService.summary());
    }

    /** 库存变更流水 */
    @GetMapping("/records")
    public Result<PageResult<StockRecord>> records(@RequestParam(required = false) Integer bizType,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(stockService.records(bizType, pageNum, pageSize));
    }
}
