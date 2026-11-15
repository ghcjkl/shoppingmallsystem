package com.shoping.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价：已完成订单按订单项评价，一项只能评一次
 */
@Data
@TableName("product_review")
public class ProductReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 订单明细 ID（唯一索引，防重复评价） */
    private Long orderItemId;

    private Long productId;

    private Long userId;

    /** 评分 1-5 */
    private Integer rating;

    private String content;

    /** 1 匿名，0 显示昵称 */
    private Integer anonymous;

    /** 1 显示，0 隐藏（管理端） */
    private Integer status;

    /** 商家回复 */
    private String reply;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;
}
