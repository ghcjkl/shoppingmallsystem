package com.shoping.module.product.controller;

import com.shoping.common.result.Result;
import com.shoping.module.product.entity.Material;
import com.shoping.module.product.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping("/list")
    public Result<List<Material>> list() {
        return Result.ok(materialService.list());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Material material) {
        materialService.create(material);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Material material) {
        materialService.update(material);
        return Result.ok();
    }

    @PostMapping("/{id}/stock")
    public Result<Void> adjustStock(@PathVariable Long id, @RequestParam BigDecimal delta) {
        materialService.adjustStock(id, delta);
        return Result.ok();
    }
}
