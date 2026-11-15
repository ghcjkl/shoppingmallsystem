package com.shoping.module.product.service;

import com.shoping.module.product.vo.IngredientVO;

import java.util.List;

public interface IngredientService {

    List<IngredientVO> list();

    List<IngredientVO> listByProduct(Long productId);
}
