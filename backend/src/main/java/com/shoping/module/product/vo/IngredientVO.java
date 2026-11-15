package com.shoping.module.product.vo;

import com.shoping.module.product.entity.Ingredient;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredientVO {

    private Long id;
    private String name;
    private BigDecimal price;
    private Long materialId;
    private Integer status;

    public static IngredientVO from(Ingredient ingredient) {
        IngredientVO vo = new IngredientVO();
        vo.setId(ingredient.getId());
        vo.setName(ingredient.getName());
        vo.setPrice(ingredient.getPrice());
        vo.setMaterialId(ingredient.getMaterialId());
        vo.setStatus(ingredient.getStatus());
        return vo;
    }
}
