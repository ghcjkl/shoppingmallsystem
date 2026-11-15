package com.shoping.module.stock.service;

import com.shoping.module.order.entity.OrderItem;

import java.util.List;

/**
 * 库存回滚服务：取消订单、售后完成（退款 / 退货退款）共用
 *
 * <p>与下单扣减（OrderServiceImpl）相互对应：下单扣一份，取消或售后完成就还一份，并写入 stock_record 流水。</p>
 */
public interface StockRollbackService {

    /**
     * 回滚订单项占用的 SKU 库存（增加可售库存）并写库存流水
     *
     * @param items  订单项列表（按数量回补）
     * @param reason 流水备注
     * @return 实际回补的商品件数
     */
    int rollbackSkuStock(List<OrderItem> items, String reason);

    /**
     * 回滚现制商品按 BOM 消耗的原料并写库存流水
     *
     * @param items  订单项列表（按 BOM 用量 × 数量回补）
     * @param reason 流水备注
     * @return 实际回补的原料条数
     */
    int rollbackMaterials(List<OrderItem> items, String reason);
}
