package com.shoping.module.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductQuery {

    /** 关键词 */
    private String keyword;

    /** 商品类型：1 标品零食，2 现制饮品 */
    private Integer productType;

    /** 分类 ID */
    private Long categoryId;

    /** 状态：1 上架，0 下架（仅管理端使用，用户端固定查上架） */
    private Integer status;

    @Min(value = 1, message = "页码最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private long pageSize = 20;
}
