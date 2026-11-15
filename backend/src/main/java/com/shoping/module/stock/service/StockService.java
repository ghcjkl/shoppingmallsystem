package com.shoping.module.stock.service;

import com.shoping.common.result.PageResult;
import com.shoping.module.product.entity.Material;
import com.shoping.module.stock.dto.StockAdjustRequest;
import com.shoping.module.stock.entity.StockRecord;
import com.shoping.module.stock.vo.SkuStockVO;
import com.shoping.module.stock.vo.StockSummaryVO;

import java.util.List;

public interface StockService {

    /** 商品库存分页（按 SKU 展开），lowOnly=true 只看低库存 */
    PageResult<SkuStockVO> skuPage(String keyword, Boolean lowOnly, long pageNum, long pageSize);

    /** 原料库存列表 */
    List<Material> materials();

    /** 库存概览 */
    StockSummaryVO summary();

    /** 调整商品 SKU 库存（入库 / 出库 / 直接设定），并写入流水 */
    void adjustSku(Long skuId, StockAdjustRequest request);

    /** 调整原料库存（入库 / 出库 / 直接设定），并写入流水 */
    void adjustMaterial(Long materialId, StockAdjustRequest request);

    /** 库存变更流水 */
    PageResult<StockRecord> records(Integer bizType, long pageNum, long pageSize);
}
