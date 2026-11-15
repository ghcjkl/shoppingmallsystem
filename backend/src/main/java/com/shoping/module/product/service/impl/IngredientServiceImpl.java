package com.shoping.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.module.product.entity.Ingredient;
import com.shoping.module.product.entity.ProductIngredient;
import com.shoping.module.product.mapper.IngredientMapper;
import com.shoping.module.product.mapper.ProductIngredientMapper;
import com.shoping.module.product.service.IngredientService;
import com.shoping.module.product.vo.IngredientVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientMapper ingredientMapper;
    private final ProductIngredientMapper productIngredientMapper;

    @Override
    public List<IngredientVO> list() {
        return ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                        .eq(Ingredient::getStatus, 1)
                        .orderByAsc(Ingredient::getId))
                .stream().map(IngredientVO::from).toList();
    }

    @Override
    public List<IngredientVO> listByProduct(Long productId) {
        List<ProductIngredient> relations = productIngredientMapper.selectList(
                new LambdaQueryWrapper<ProductIngredient>().eq(ProductIngredient::getProductId, productId));
        return relations.stream().map(rel -> {
            Ingredient ingredient = ingredientMapper.selectById(rel.getIngredientId());
            if (ingredient == null) {
                return null;
            }
            IngredientVO vo = IngredientVO.from(ingredient);
            // 商品级加料价可覆盖默认加料价
            if (rel.getPrice() != null) {
                vo.setPrice(rel.getPrice());
            }
            return vo;
        }).filter(java.util.Objects::nonNull).toList();
    }
}
