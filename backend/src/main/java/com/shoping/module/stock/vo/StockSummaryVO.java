package com.shoping.module.stock.vo;

import lombok.Data;

/**
 * 库存概览
 */
@Data
public class StockSummaryVO {

    /** SKU 总数 */
    private long skuTotal;

    /** 低库存 SKU 数（0 < 库存 <= 阈值） */
    private long skuLow;

    /** 缺货 SKU 数（库存 <= 0） */
    private long skuOut;

    /** 原料总数 */
    private long materialTotal;

    /** 低于安全库存的原料数 */
    private long materialLow;

    /** 低库存阈值（SKU） */
    private int lowThreshold;
}
