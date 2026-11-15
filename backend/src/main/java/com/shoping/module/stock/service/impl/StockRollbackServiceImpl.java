package com.shoping.module.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.context.AuthContext;
import com.shoping.module.order.entity.OrderItem;
import com.shoping.module.product.entity.Material;
import com.shoping.module.product.entity.ProductMaterial;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.mapper.MaterialMapper;
import com.shoping.module.product.mapper.ProductMaterialMapper;
import com.shoping.module.product.mapper.ProductSkuMapper;
import com.shoping.module.stock.entity.StockRecord;
import com.shoping.module.stock.mapper.StockRecordMapper;
import com.shoping.module.stock.service.StockRollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存回滚实现：SKU 库存 + 现制原料 BOM，变更全部落到 stock_record
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRollbackServiceImpl implements StockRollbackService {

    /** 流水对象类型 */
    private static final int BIZ_SKU = 1;
    private static final int BIZ_MATERIAL = 2;

    /** 流水变更方式：入库 */
    private static final int CHANGE_IN = 1;

    private final ProductSkuMapper productSkuMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final MaterialMapper materialMapper;
    private final StockRecordMapper stockRecordMapper;

    @Override
    @Transactional
    public int rollbackSkuStock(List<OrderItem> items, String reason) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (OrderItem item : items) {
            if (item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            if (sku == null) {
                log.warn("库存回滚跳过：SKU 不存在 skuId={}, orderItemId={}", item.getSkuId(), item.getId());
                continue;
            }
            int qty = item.getQuantity();
            // 原子加库存：并发取消/售后同时回补时不会互相覆盖
            productSkuMapper.increaseStock(sku.getId(), qty);
            ProductSku latest = productSkuMapper.selectById(sku.getId());
            int after = latest == null || latest.getStock() == null ? qty : latest.getStock();
            int before = after - qty;

            writeRecord(BIZ_SKU, sku.getId(), skuName(item, sku), BigDecimal.valueOf(qty),
                    BigDecimal.valueOf(before), BigDecimal.valueOf(after), reason);
            total += qty;
        }
        return total;
    }

    @Override
    @Transactional
    public int rollbackMaterials(List<OrderItem> items, String reason) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (OrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            List<ProductMaterial> boms = productMaterialMapper.selectList(
                    new LambdaQueryWrapper<ProductMaterial>()
                            .eq(ProductMaterial::getProductId, item.getProductId())
                            .and(w -> w.eq(ProductMaterial::getSkuId, item.getSkuId())
                                    .or().isNull(ProductMaterial::getSkuId)));
            for (ProductMaterial bom : boms) {
                Material material = materialMapper.selectById(bom.getMaterialId());
                if (material == null || bom.getQuantity() == null) {
                    continue;
                }
                BigDecimal used = bom.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity()));
                materialMapper.increaseStock(material.getId(), used);
                Material latest = materialMapper.selectById(material.getId());
                BigDecimal after = latest == null || latest.getStock() == null ? used : latest.getStock();
                BigDecimal before = after.subtract(used);

                String name = material.getName();
                if (StringUtils.hasText(material.getUnit())) {
                    name = name + "（" + material.getUnit() + "）";
                }
                writeRecord(BIZ_MATERIAL, material.getId(), name, used, before, after, reason);
                count++;
            }
        }
        return count;
    }

    private String skuName(OrderItem item, ProductSku sku) {
        String name = StringUtils.hasText(item.getProductName()) ? item.getProductName() : "商品";
        String spec = StringUtils.hasText(item.getSkuSpec()) ? item.getSkuSpec() : sku.getSkuCode();
        if (StringUtils.hasText(spec)) {
            name = name + " / " + spec;
        }
        return name.length() > 150 ? name.substring(0, 150) : name;
    }

    /** 库存流水（changeQty 统一记正数，changeType 表示方向） */
    private void writeRecord(int bizType, Long targetId, String targetName, BigDecimal qty,
                             BigDecimal before, BigDecimal after, String reason) {
        StockRecord record = new StockRecord();
        record.setBizType(bizType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setChangeType(CHANGE_IN);
        record.setChangeQty(qty);
        record.setBeforeQty(before);
        record.setAfterQty(after);
        record.setReason(reason);
        record.setOperatorId(AuthContext.userId());
        stockRecordMapper.insert(record);
    }
}
