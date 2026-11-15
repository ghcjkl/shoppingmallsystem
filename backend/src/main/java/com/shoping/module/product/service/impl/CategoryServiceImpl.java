package com.shoping.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.product.entity.Category;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.mapper.CategoryMapper;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CacheService cacheService;

    @Override
    public List<Category> list(Integer productType, boolean includeDisabled) {
        String key = CacheKeys.categoryList(productType, includeDisabled);
        return cacheService.remember(key, CacheKeys.TTL_CATEGORY,
                new TypeReference<List<Category>>() {
                }, () -> loadList(productType, includeDisabled));
    }

    private List<Category> loadList(Integer productType, boolean includeDisabled) {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(productType != null, Category::getProductType, productType)
                .eq(!includeDisabled, Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        // 附带商品数：管理端判断能不能删、停用前看看影响面
        for (Category category : categories) {
            category.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                    .eq(Product::getCategoryId, category.getId())).intValue());
        }
        return categories;
    }

    @Override
    public Category create(Category category) {
        String name = normalizeName(category.getName());
        Integer type = category.getProductType();
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        ensureNameFree(name, type, null);
        category.setId(null);
        category.setName(name);
        category.setParentId(category.getParentId() == null ? 0L : category.getParentId());
        category.setStatus(category.getStatus() == null ? 1 : category.getStatus());
        if (category.getSort() == null) {
            category.setSort(nextSort(type));
        }
        category.setCreateTime(null);
        category.setUpdateTime(null);
        categoryMapper.insert(category);
        evictLists();
        return categoryMapper.selectById(category.getId());
    }

    @Override
    public Category update(Category category) {
        if (category.getId() == null || categoryMapper.selectById(category.getId()) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Category current = categoryMapper.selectById(category.getId());
        String name = normalizeName(category.getName());
        Integer type = category.getProductType() == null ? current.getProductType() : category.getProductType();
        ensureNameFree(name, type, category.getId());
        category.setName(name);
        category.setProductType(type);
        // 名称/排序/状态以外的时间字段不允许由前端覆盖
        category.setCreateTime(null);
        category.setUpdateTime(null);
        categoryMapper.updateById(category);
        evictLists();
        return categoryMapper.selectById(category.getId());
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Long used = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
        evictLists();
    }

    /** 分类是商品导航的数据源，任何变更都清掉全部分类列表缓存 */
    private void evictLists() {
        cacheService.evictByPrefix(CacheKeys.CATEGORY_LIST);
    }

    private String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (name.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        return name;
    }

    /** 同一类型下分类名不允许重复 */
    private void ensureNameFree(String name, Integer productType, Long excludeId) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)
                .eq(Category::getProductType, productType)
                .ne(excludeId != null, Category::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
    }

    private int nextSort(Integer productType) {
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getProductType, productType)
                .orderByDesc(Category::getSort)
                .last("limit 1"));
        return list.isEmpty() || list.get(0).getSort() == null ? 0 : list.get(0).getSort() + 1;
    }
}
