package com.shoping.module.product.service;

import com.shoping.module.product.entity.Material;

import java.math.BigDecimal;
import java.util.List;

public interface MaterialService {

    List<Material> list();

    void create(Material material);

    void update(Material material);

    /** 库存调整：delta 为正入库、为负扣减；原料售罄自动下架关联商品 */
    void adjustStock(Long id, BigDecimal delta);
}
