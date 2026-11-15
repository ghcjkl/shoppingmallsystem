package com.shoping.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提交评价（用户端） */
@Data
public class ReviewCreateRequest {

    /** 订单明细 ID：一项一评 */
    @NotNull(message = "请指定要评价的商品")
    private Long orderItemId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分为 1-5 星")
    @Max(value = 5, message = "评分为 1-5 星")
    private Integer rating;

    @Size(max = 500, message = "评价内容最多 500 字")
    private String content;

    /** 是否匿名 */
    private Boolean anonymous;
}
