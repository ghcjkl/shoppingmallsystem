package com.shoping.module.review.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.module.review.dto.ReviewCreateRequest;
import com.shoping.module.review.service.ReviewService;
import com.shoping.module.review.vo.ProductReviewPageVO;
import com.shoping.module.review.vo.ProductReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 商品评价
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 提交评价（已完成订单） */
    @PostMapping
    public Result<ProductReviewVO> create(@Valid @RequestBody ReviewCreateRequest request) {
        return Result.ok(reviewService.create(AuthContext.requireUserId(), request));
    }

    /** 商品评价列表（公开）：评分汇总 + 分页 */
    @GetMapping("/product/{productId}")
    public Result<ProductReviewPageVO> byProduct(@PathVariable Long productId,
                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) Integer rating) {
        return Result.ok(reviewService.pageByProduct(productId, pageNum, pageSize, rating));
    }

    /** 某订单下我已提交的评价（订单详情显示「已评价」） */
    @GetMapping("/order/{orderId}")
    public Result<List<ProductReviewVO>> byOrder(@PathVariable Long orderId) {
        return Result.ok(reviewService.listByOrder(AuthContext.requireUserId(), orderId));
    }
}
