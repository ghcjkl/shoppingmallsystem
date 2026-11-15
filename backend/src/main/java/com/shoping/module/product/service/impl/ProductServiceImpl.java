package com.shoping.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.common.result.PageResult;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.mapper.ProductSkuMapper;
import com.shoping.module.product.service.IngredientService;
import com.shoping.module.product.service.ProductService;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;
import com.shoping.module.review.service.ReviewService;
import com.shoping.module.review.vo.ProductRatingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final IngredientService ingredientService;
    private final ReviewService reviewService;
    private final CacheService cacheService;

    @Override
    public PageResult<ProductVO> page(ProductQuery query) {
        String key = CacheKeys.productPage(query.getProductType(), query.getCategoryId(),
                query.getKeyword(), query.getPageNum(), query.getPageSize());
        return cacheService.remember(key, CacheKeys.TTL_PRODUCT_PAGE,
                new TypeReference<PageResult<ProductVO>>() {
                }, () -> loadPage(query));
    }

    private PageResult<ProductVO> loadPage(ProductQuery query) {
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(query.getProductType() != null, Product::getProductType, query.getProductType())
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(Product::getStatus, 1)
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), Product::getName, query.getKeyword())
                .orderByDesc(Product::getCreateTime);
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);
        List<ProductVO> records = productPage.getRecords().stream().map(ProductVO::from).toList();
        fillStock(records);
        fillRating(records);
        PageResult<ProductVO> result = new PageResult<>();
        result.setTotal(productPage.getTotal());
        result.setPageNum(productPage.getCurrent());
        result.setPageSize(productPage.getSize());
        result.setRecords(records);
        return result;
    }

    /** 批量填充可用库存（启用 SKU 库存合计），0 即售罄 */
    private void fillStock(List<ProductVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(ProductVO::getId).toList();
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .in(ProductSku::getProductId, ids)
                .eq(ProductSku::getStatus, 1));
        Map<Long, Integer> stockMap = new HashMap<>();
        for (ProductSku sku : skus) {
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            stockMap.merge(sku.getProductId(), stock, Integer::sum);
        }
        for (ProductVO vo : records) {
            vo.setStock(stockMap.getOrDefault(vo.getId(), 0));
        }
    }

    /** 批量填充评分：无评价的商品评分 0、条数 0 */
    private void fillRating(List<ProductVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(ProductVO::getId).toList();
        Map<Long, ProductRatingVO> ratings = reviewService.ratings(ids);
        for (ProductVO vo : records) {
            ProductRatingVO rating = ratings.get(vo.getId());
            vo.setRating(rating == null ? BigDecimal.ZERO : rating.getRating());
            vo.setReviewCount(rating == null || rating.getReviewCount() == null
                    ? 0 : rating.getReviewCount().intValue());
        }
    }

    @Override
    public ProductVO getById(Long id) {
        ProductVO vo = ProductVO.from(getProduct(id));
        ProductRatingVO rating = reviewService.rating(id);
        vo.setRating(rating == null || rating.getRating() == null ? BigDecimal.ZERO : rating.getRating());
        vo.setReviewCount(rating == null || rating.getReviewCount() == null
                ? 0 : rating.getReviewCount().intValue());
        return vo;
    }

    @Override
    public ProductDetailVO detail(Long id) {
        return cacheService.remember(CacheKeys.productDetail(id), CacheKeys.TTL_PRODUCT_DETAIL,
                new TypeReference<ProductDetailVO>() {
                }, () -> loadDetail(id));
    }

    private ProductDetailVO loadDetail(Long id) {
        Product product = getProduct(id);
        ProductDetailVO detail = new ProductDetailVO();
        copyBase(detail, ProductVO.from(product));
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, id)
                .eq(ProductSku::getStatus, 1)
                .orderByAsc(ProductSku::getId));
        detail.setSkus(skus);
        detail.setIngredients(ingredientService.listByProduct(id));
        detail.setStock(skus.stream().mapToInt(s -> s.getStock() == null ? 0 : s.getStock()).sum());
        ProductRatingVO rating = reviewService.rating(id);
        detail.setRating(rating == null || rating.getRating() == null ? BigDecimal.ZERO : rating.getRating());
        detail.setReviewCount(rating == null || rating.getReviewCount() == null
                ? 0 : rating.getReviewCount().intValue());
        return detail;
    }

    private Product getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void copyBase(ProductDetailVO target, ProductVO source) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setProductType(source.getProductType());
        target.setCategoryId(source.getCategoryId());
        target.setCoverImg(source.getCoverImg());
        target.setDetail(source.getDetail());
        target.setBasePrice(source.getBasePrice());
        target.setStatus(source.getStatus());
        target.setNetContent(source.getNetContent());
        target.setShelfLife(source.getShelfLife());
        target.setAllergen(source.getAllergen());
        target.setEstMakeMinutes(source.getEstMakeMinutes());
        target.setCalories(source.getCalories());
        target.setSugarNote(source.getSugarNote());
    }
}
