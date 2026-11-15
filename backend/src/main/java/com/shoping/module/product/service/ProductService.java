package com.shoping.module.product.service;

import com.shoping.common.result.PageResult;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;

public interface ProductService {

    PageResult<ProductVO> page(ProductQuery query);

    ProductVO getById(Long id);

    ProductDetailVO detail(Long id);
}
