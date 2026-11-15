package com.shoping.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 商家回复评价 */
@Data
public class ReviewReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 255, message = "回复最多 255 字")
    private String reply;
}
