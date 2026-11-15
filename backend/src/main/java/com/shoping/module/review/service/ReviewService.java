package com.shoping.module.review.service;

import com.shoping.common.result.PageResult;
import com.shoping.module.review.dto.ReviewCreateRequest;
import com.shoping.module.review.vo.ProductRatingVO;
import com.shoping.module.review.vo.ProductReviewPageVO;
import com.shoping.module.review.vo.ProductReviewVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ReviewService {

    /** 批量评分：商品列表页一次查完，避免 N+1 */
    Map<Long, ProductRatingVO> ratings(Collection<Long> productIds);

    /** 单个商品评分汇总 */
    ProductRatingVO rating(Long productId);

    /** 用户提交评价：仅限自己已完成订单中的商品，一项一评 */
    ProductReviewVO create(Long userId, ReviewCreateRequest request);

    /** 商品页评价（公开）：评分汇总 + 分页列表 */
    ProductReviewPageVO pageByProduct(Long productId, int pageNum, int pageSize, Integer rating);

    /** 订单详情用：该订单下我已提交的评价 */
    List<ProductReviewVO> listByOrder(Long userId, Long orderId);

    // ===== 管理端 =====
    PageResult<ProductReviewVO> pageAdmin(Long productId, Integer status, Integer rating,
                                          String keyword, int pageNum, int pageSize);

    /** 显示 / 隐藏评价 */
    void updateStatus(Long id, Integer status);

    /** 商家回复（传空串表示清除回复） */
    ProductReviewVO reply(Long id, String reply);

    void delete(Long id);
}
