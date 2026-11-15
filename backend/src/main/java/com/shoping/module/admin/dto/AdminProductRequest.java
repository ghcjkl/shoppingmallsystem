package com.shoping.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端商品新增/编辑请求
 */
@Data
public class AdminProductRequest {

    /** 编辑时必传；新增留空 */
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称不能超过 128 字")
    private String name;

    /** 商品类型：1 标品零食，2 现制饮品 */
    @NotNull(message = "商品类型不能为空")
    @Min(value = 1, message = "商品类型不合法")
    private Integer productType;

    private Long categoryId;

    /** 主图地址（先调上传接口拿到 url 再回填） */
    private String coverImg;

    private String detail;

    @NotNull(message = "基准价不能为空")
    @DecimalMin(value = "0", message = "基准价不能为负")
    private BigDecimal basePrice;

    /** 状态：1 上架，0 下架；新增不传默认上架 */
    private Integer status;

    private String netContent;

    private String shelfLife;

    private String allergen;

    private Integer estMakeMinutes;

    private Integer calories;

    private String sugarNote;

    /** 未传 skus 时自动创建的默认规格库存 */
    @Min(value = 0, message = "库存不能为负")
    private Integer defaultStock;

    /** 规格列表，编辑时按 id 更新、无 id 新增 */
    private List<AdminSkuRequest> skus;
}
