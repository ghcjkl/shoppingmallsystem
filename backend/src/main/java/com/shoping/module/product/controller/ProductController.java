package com.shoping.module.product.controller;

import com.shoping.common.result.PageResult;
import com.shoping.common.result.Result;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.service.ProductService;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<PageResult<ProductVO>> page(@Valid ProductQuery query) {
        return Result.ok(productService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ProductVO> get(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }
}
