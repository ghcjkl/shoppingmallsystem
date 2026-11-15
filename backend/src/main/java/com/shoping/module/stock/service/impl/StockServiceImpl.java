package com.shoping.module.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.context.AuthContext;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.common.result.PageResult;
import com.shoping.module.product.entity.Material;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.mapper.MaterialMapper;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.mapper.ProductSkuMapper;
import com.shoping.module.product.service.MaterialService;
import com.shoping.module.stock.dto.StockAdjustRequest;
import com.shoping.module.stock.entity.StockRecord;
import com.shoping.module.stock.mapper.StockRecordMapper;
import com.shoping.module.stock.service.StockService;
import com.shoping.module.stock.vo.SkuStockVO;
import com.shoping.module.stock.vo.StockSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    /** SKU 低库存阈值（件） */
    private static final int LOW_THRESHOLD = 10;

    /** 原料默认安全库存（未配置时） */
    private static final BigDecimal DEFAULT_SAFE_STOCK = BigDecimal.TEN;

    private static final int BIZ_SKU = 1;
    private static final int BIZ_MATERIAL = 2;

    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final MaterialMapper materialMapper;
    private final MaterialService materialService;
    private final StockRecordMapper stockRecordMapper;
    private final CacheService cacheService;

    @Override
    public PageResult<SkuStockVO> skuPage(String keyword, Boolean lowOnly, long pageNum, long pageSize) {
        Page<ProductSku> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                            .select(Product::getId)
                            .like(Product::getName, kw))
                    .stream().map(Product::getId).toList();
            wrapper.and(q -> {
                q.like(ProductSku::getSkuCode, kw);
                if (!productIds.isEmpty()) {
                    q.or().in(ProductSku::getProductId, productIds);
                }
            });
        }
        if (Boolean.TRUE.equals(lowOnly)) {
            wrapper.le(ProductSku::getStock, LOW_THRESHOLD);
        }
        wrapper.orderByAsc(ProductSku::getStock).orderByAsc(ProductSku::getId);

        IPage<ProductSku> result = skuMapper.selectPage(page, wrapper);
        List<Long> ids = result.getRecords().stream().map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productMap = ids.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<SkuStockVO> records = result.getRecords().stream().map(sku -> {
            SkuStockVO vo = new SkuStockVO();
            vo.setSkuId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setSkuCode(sku.getSkuCode());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock() == null ? 0 : sku.getStock());
            vo.setSkuStatus(sku.getStatus());
            Product product = productMap.get(sku.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductType(product.getProductType());
                vo.setProductStatus(product.getStatus());
            }
            return vo;
        }).toList();

        PageResult<SkuStockVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setRecords(records);
        return pageResult;
    }

    @Override
    public List<Material> materials() {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .orderByAsc(Material::getId));
    }

    @Override
    public StockSummaryVO summary() {
        StockSummaryVO vo = new StockSummaryVO();
        vo.setLowThreshold(LOW_THRESHOLD);
        vo.setSkuTotal(skuMapper.selectCount(null));
        vo.setSkuOut(countOf(skuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                .le(ProductSku::getStock, 0))));
        long low = countOf(skuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                .gt(ProductSku::getStock, 0)
                .le(ProductSku::getStock, LOW_THRESHOLD)));
        vo.setSkuLow(low);

        List<Material> materials = materials();
        vo.setMaterialTotal(materials.size());
        vo.setMaterialLow(materials.stream().filter(m -> {
            BigDecimal safe = m.getSafeStock() == null ? DEFAULT_SAFE_STOCK : m.getSafeStock();
            BigDecimal stock = m.getStock() == null ? BigDecimal.ZERO : m.getStock();
            return stock.compareTo(safe) <= 0;
        }).count());
        return vo;
    }

    @Override
    @Transactional
    public void adjustSku(Long skuId, StockAdjustRequest request) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        int before = sku.getStock() == null ? 0 : sku.getStock();
        int qty = request.getQuantity().intValue();
        int after = switch (request.getChangeType()) {
            case 1 -> {
                if (qty <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "入库数量需大于 0");
                }
                yield before + qty;
            }
            case 2 -> {
                if (qty <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "出库数量需大于 0");
                }
                if (qty > before) {
                    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "出库数量超过当前库存（" + before + "）");
                }
                yield before - qty;
            }
            default -> qty;
        };
        // 乐观并发控制：库存被别人改过就要求刷新重试
        int rows = skuMapper.update(null, new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .eq(ProductSku::getStock, before)
                .set(ProductSku::getStock, after));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "库存刚刚被其他人修改，请刷新后重试");
        }
        String name = (product == null ? "商品" : product.getName())
                + (StringUtils.hasText(sku.getSkuCode()) ? " / " + sku.getSkuCode() : "");
        writeRecord(BIZ_SKU, skuId, name, request, BigDecimal.valueOf(before), BigDecimal.valueOf(after));
        // 前台列表与详情都带可用库存，调库存后立即失效，避免还显示旧库存
        evictSkuCache(sku.getProductId());
    }

    private void evictSkuCache(Long productId) {
        if (productId != null) {
            cacheService.evict(CacheKeys.productDetail(productId));
        }
        cacheService.evictByPrefix(CacheKeys.PRODUCT_PAGE);
    }

    @Override
    @Transactional
    public void adjustMaterial(Long materialId, StockAdjustRequest request) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        BigDecimal before = material.getStock() == null ? BigDecimal.ZERO : material.getStock();
        BigDecimal qty = request.getQuantity();
        BigDecimal delta = switch (request.getChangeType()) {
            case 1 -> {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "入库数量需大于 0");
                }
                yield qty;
            }
            case 2 -> {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "出库数量需大于 0");
                }
                yield qty.negate();
            }
            default -> qty.subtract(before);
        };
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            // 复用原料服务：含负库存校验与「原料清零自动下架关联商品」
            materialService.adjustStock(materialId, delta);
        }
        BigDecimal after = before.add(delta);
        writeRecord(BIZ_MATERIAL, materialId, material.getName(), request, before, after);
    }

    @Override
    public PageResult<StockRecord> records(Integer bizType, long pageNum, long pageSize) {
        Page<StockRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockRecord> wrapper = new LambdaQueryWrapper<StockRecord>()
                .eq(bizType != null, StockRecord::getBizType, bizType)
                .orderByDesc(StockRecord::getId);
        IPage<StockRecord> result = stockRecordMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    private void writeRecord(int bizType, Long targetId, String targetName,
                             StockAdjustRequest request, BigDecimal before, BigDecimal after) {
        StockRecord record = new StockRecord();
        record.setBizType(bizType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setChangeType(request.getChangeType());
        record.setChangeQty(after.subtract(before));
        record.setBeforeQty(before);
        record.setAfterQty(after);
        record.setReason(request.getReason());
        record.setOperatorId(AuthContext.userId());
        stockRecordMapper.insert(record);
    }

    private long countOf(Long value) {
        return value == null ? 0L : value;
    }
}
