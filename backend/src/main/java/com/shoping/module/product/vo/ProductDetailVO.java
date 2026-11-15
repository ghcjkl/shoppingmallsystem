package com.shoping.module.product.vo;

import com.shoping.module.product.entity.ProductSku;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品详情（含 SKU 与可选加料）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailVO extends ProductVO {

    private List<ProductSku> skus;
    private List<IngredientVO> ingredients;
}
