package com.shoping.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.product.entity.Material;
import com.shoping.module.product.entity.Product;
import com.shoping.module.product.entity.ProductMaterial;
import com.shoping.module.product.mapper.MaterialMapper;
import com.shoping.module.product.mapper.ProductMapper;
import com.shoping.module.product.mapper.ProductMaterialMapper;
import com.shoping.module.product.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final ProductMapper productMapper;

    @Override
    public List<Material> list() {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getStatus, 1)
                .orderByAsc(Material::getId));
    }

    @Override
    public void create(Material material) {
        material.setId(null);
        materialMapper.insert(material);
    }

    @Override
    public void update(Material material) {
        if (material.getId() == null || materialMapper.selectById(material.getId()) == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        materialMapper.updateById(material);
    }

    @Override
    @Transactional
    public void adjustStock(Long id, BigDecimal delta) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        BigDecimal newStock = material.getStock().add(delta);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_ENOUGH);
        }
        material.setStock(newStock);
        materialMapper.updateById(material);
        if (newStock.compareTo(BigDecimal.ZERO) == 0) {
            offlineRelatedProducts(id);
        }
    }

    /** 原料售罄 -> 关联商品自动下架（沽清联动） */
    private void offlineRelatedProducts(Long materialId) {
        List<ProductMaterial> boms = productMaterialMapper.selectList(
                new LambdaQueryWrapper<ProductMaterial>().eq(ProductMaterial::getMaterialId, materialId));
        for (ProductMaterial bom : boms) {
            Product product = productMapper.selectById(bom.getProductId());
            if (product != null && product.getStatus() != null && product.getStatus() == 1) {
                product.setStatus(0);
                productMapper.updateById(product);
            }
        }
    }
}
