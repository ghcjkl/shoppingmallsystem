package com.shoping.module.product.controller;

import com.shoping.common.result.Result;
import com.shoping.module.product.service.IngredientService;
import com.shoping.module.product.vo.IngredientVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingredient")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping("/list")
    public Result<List<IngredientVO>> list() {
        return Result.ok(ingredientService.list());
    }

    @GetMapping("/product/{productId}")
    public Result<List<IngredientVO>> listByProduct(@PathVariable Long productId) {
        return Result.ok(ingredientService.listByProduct(productId));
    }
}
