package com.shoping.module.product.service;

import com.shoping.module.product.entity.Category;

import java.util.List;

public interface CategoryService {

    /** 分类列表；includeDisabled=false 时只返回启用的（顾客端用） */
    List<Category> list(Integer productType, boolean includeDisabled);

    Category create(Category category);

    Category update(Category category);

    void delete(Long id);
}
