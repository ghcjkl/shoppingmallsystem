package com.shoping.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 评价展示对象（前台列表 / 管理端列表共用） */
@Data
public class ProductReviewVO {

    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long productId;
    private Long userId;

    /** 商品名（管理端展示用） */
    private String productName;

    private Integer rating;
    private String content;
    private Integer anonymous;
    private Integer status;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;

    /** 昵称：匿名时由服务层置为「匿名用户」 */
    private String nickname;
}
