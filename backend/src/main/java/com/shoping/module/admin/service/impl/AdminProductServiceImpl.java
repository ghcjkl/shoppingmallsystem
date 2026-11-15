package com.shoping.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.common.result.PageResult;
import com.shoping.module.admin.dto.AdminProductRequest;
import com.shoping.module.admin.dto.AdminSkuRequest;
import com.shoping.module.admin.service.AdminProductService;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.entity.Category;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.mapper.CategoryMapper;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.mapper.ProductSkuMapper;
import com.shoping.module.product.service.IngredientService;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final IngredientService ingredientService;
    private final CacheService cacheService;

    @Override
    public PageResult<ProductVO> page(ProductQuery query) {
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(query.getProductType() != null, Product::getProductType, query.getProductType())
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(),
                        Product::getName, query.getKeyword())
                .orderByDesc(Product::getId);
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);
        return PageResult.of(productPage.convert(ProductVO::from));
    }

    @Override
    public ProductDetailVO detail(Long id) {
        Product product = getProduct(id);
        ProductDetailVO detail = new ProductDetailVO();
        BeanUtils.copyProperties(ProductVO.from(product), detail);
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, id)
                .orderByAsc(ProductSku::getId));
        detail.setSkus(skus);
        detail.setIngredients(ingredientService.listByProduct(id));
        return detail;
    }

    @Override
    @Transactional
    public ProductDetailVO create(AdminProductRequest request) {
        validate(request);
        Product product = new Product();
        apply(product, request);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        productMapper.insert(product);
        saveSkus(product, request, true);
        evictProductCache();
        return detail(product.getId());
    }

    @Override
    @Transactional
    public ProductDetailVO update(AdminProductRequest request) {
        Product product = getProduct(request.getId());
        validate(request);
        apply(product, request);
        productMapper.updateById(product);
        saveSkus(product, request, false);
        evictProductCache();
        return detail(product.getId());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Product product = getProduct(id);
        product.setStatus(status);
        productMapper.updateById(product);
        evictProductCache();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getProduct(id);
        productMapper.deleteById(id);
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        evictProductCache();
    }

    /** 商品上下架/改名/改价/改 SKU 都会影响前台列表与详情，整片商品缓存统一失效 */
    private void evictProductCache() {
        cacheService.evictByPrefix(CacheKeys.PRODUCT_ALL);
    }

    private void apply(Product product, AdminProductRequest request) {
        product.setName(request.getName().trim());
        product.setProductType(request.getProductType());
        product.setCategoryId(request.getCategoryId());
        product.setCoverImg(request.getCoverImg());
        product.setDetail(request.getDetail());
        product.setBasePrice(request.getBasePrice());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        product.setNetContent(request.getNetContent());
        product.setShelfLife(request.getShelfLife());
        product.setAllergen(request.getAllergen());
        product.setEstMakeMinutes(request.getEstMakeMinutes());
        product.setCalories(request.getCalories());
        product.setSugarNote(request.getSugarNote());
    }

    private void saveSkus(Product product, AdminProductRequest request, boolean isCreate) {
        List<AdminSkuRequest> skus = request.getSkus();
        if (skus == null || skus.isEmpty()) {
            // 新增且未指定规格：自动补一个默认规格，保证商品可直接下单
            if (isCreate) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSkuCode("默认");
                sku.setPrice(product.getBasePrice());
                sku.setStock(request.getDefaultStock() == null ? 0 : request.getDefaultStock());
                sku.setStatus(1);
                productSkuMapper.insert(sku);
            }
            return;
        }
        for (AdminSkuRequest item : skus) {
            if (item.getId() == null) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSkuCode(hasText(item.getSkuCode()) ? item.getSkuCode().trim() : "默认");
                sku.setPrice(item.getPrice() == null ? product.getBasePrice() : item.getPrice());
                sku.setStock(item.getStock() == null ? 0 : item.getStock());
                sku.setStatus(item.getStatus() == null ? 1 : item.getStatus());
                productSkuMapper.insert(sku);
            } else {
                ProductSku sku = productSkuMapper.selectById(item.getId());
                if (sku == null || !product.getId().equals(sku.getProductId())) {
                    throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
                }
                if (hasText(item.getSkuCode())) {
                    sku.setSkuCode(item.getSkuCode().trim());
                }
                if (item.getPrice() != null) {
                    sku.setPrice(item.getPrice());
                }
                if (item.getStock() != null) {
                    sku.setStock(item.getStock());
                }
                if (item.getStatus() != null) {
                    sku.setStatus(item.getStatus());
                }
                productSkuMapper.updateById(sku);
            }
        }
    }

    private void validate(AdminProductRequest request) {
        if (request.getProductType() == null
                || (request.getProductType() != 1 && request.getProductType() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (request.getBasePrice() == null || request.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
    }

    private Product getProduct(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
