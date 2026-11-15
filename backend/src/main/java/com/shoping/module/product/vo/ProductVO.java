package com.shoping.module.product.vo;

import com.shoping.module.product.entity.Product;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVO {

    private Long id;
    private String name;
    private Integer productType;
    private Long categoryId;
    private String coverImg;
    private String detail;
    private BigDecimal basePrice;
    private Integer status;
    private String netContent;
    private String shelfLife;
    private String allergen;
    private Integer estMakeMinutes;
    private Integer calories;
    private String sugarNote;

    /** 可用库存（启用规格库存合计，0 表示售罄；列表页用于展示售罄并禁止加购） */
    private Integer stock;

    /** 平均评分（1 位小数，无评价时为 0） */
    private BigDecimal rating;

    /** 评价条数（仅统计显示中的评价） */
    private Integer reviewCount;

    public static ProductVO from(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setProductType(product.getProductType());
        vo.setCategoryId(product.getCategoryId());
        vo.setCoverImg(product.getCoverImg());
        vo.setDetail(product.getDetail());
        vo.setBasePrice(product.getBasePrice());
        vo.setStatus(product.getStatus());
        vo.setNetContent(product.getNetContent());
        vo.setShelfLife(product.getShelfLife());
        vo.setAllergen(product.getAllergen());
        vo.setEstMakeMinutes(product.getEstMakeMinutes());
        vo.setCalories(product.getCalories());
        vo.setSugarNote(product.getSugarNote());
        return vo;
    }
}
