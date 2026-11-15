package com.shoping.module.admin.controller;

import com.shoping.common.file.FileStorageService;
import com.shoping.common.file.UploadResult;
import com.shoping.common.result.PageResult;
import com.shoping.common.result.Result;
import com.shoping.module.admin.dto.AdminProductRequest;
import com.shoping.module.admin.service.AdminProductService;
import com.shoping.module.product.dto.ProductQuery;
import com.shoping.module.product.vo.ProductDetailVO;
import com.shoping.module.product.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端 - 商品管理（含图片上传）
 */
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final FileStorageService fileStorageService;

    /** 商品列表，含已下架商品 */
    @GetMapping("/page")
    public Result<PageResult<ProductVO>> page(@Valid ProductQuery query) {
        return Result.ok(adminProductService.page(query));
    }

    /** 商品详情，含禁用的 SKU */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminProductService.detail(id));
    }

    /** 新增商品，可同时提交 SKU；未提交则自动建默认规格 */
    @PostMapping
    public Result<ProductDetailVO> create(@Valid @RequestBody AdminProductRequest request) {
        return Result.ok(adminProductService.create(request));
    }

    /** 编辑商品 */
    @PutMapping("/{id}")
    public Result<ProductDetailVO> update(@PathVariable Long id, @Valid @RequestBody AdminProductRequest request) {
        request.setId(id);
        return Result.ok(adminProductService.update(request));
    }

    /** 上下架：status=1 上架，0 下架 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminProductService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return Result.ok();
    }

    /**
     * 上传商品图片，返回可直接入库的 url（相对路径）
     * 前端先传图拿 url，再调新增/编辑商品接口写入 coverImg
     */
    @PostMapping("/image")
    public Result<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.saveImage(file, "product"));
    }
}
