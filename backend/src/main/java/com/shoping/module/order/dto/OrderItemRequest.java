package com.shoping.module.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderItemRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** SKU ID，可为空（使用商品基准价） */
    private Long skuId;

    @Min(value = 1, message = "数量最小为 1")
    private Integer quantity = 1;

    /** 加料 ID 列表（现制） */
    private List<Long> ingredientIds;

    /** 制作选项 ID 列表（现制：温度/甜度/冰量等，每组至多一个；只传 ID，名称与加价由服务端读取） */
    private List<Long> specOptionIds;
}
