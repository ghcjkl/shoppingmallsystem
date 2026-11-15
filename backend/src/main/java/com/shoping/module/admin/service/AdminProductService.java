package com.shoping.module.admin.service;

import com.shoping.common.result.PageResult;
import com.shoping.module.admin.dto.AdminProductRequest;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;

/**
 * 管理端商品维护
 */
public interface AdminProductService {

    /** 商品列表（含已下架） */
    PageResult<ProductVO> page(ProductQuery query);

    /** 商品详情（含禁用 SKU） */
    ProductDetailVO detail(Long id);

    ProductDetailVO create(AdminProductRequest request);

    ProductDetailVO update(AdminProductRequest request);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
