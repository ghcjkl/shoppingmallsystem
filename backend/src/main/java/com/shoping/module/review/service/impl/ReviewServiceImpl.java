package com.shoping.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.common.result.PageResult;
import com.shoping.module.order.entity.OrderItem;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrderItemMapper;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.review.dto.ReviewCreateRequest;
import com.shoping.module.review.entity.ProductReview;
import com.shoping.module.review.mapper.ProductReviewMapper;
import com.shoping.module.review.service.ReviewService;
import com.shoping.module.review.vo.ProductRatingVO;
import com.shoping.module.review.vo.ProductReviewPageVO;
import com.shoping.module.review.vo.ProductReviewVO;
import com.shoping.module.review.vo.StarCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** 订单状态：5 已完成才允许评价 */
    private static final int ORDER_FINISHED = 5;

    private final ProductReviewMapper reviewMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrdersMapper ordersMapper;
    private final CacheService cacheService;

    @Override
    public Map<Long, ProductRatingVO> ratings(Collection<Long> productIds) {
        Map<Long, ProductRatingVO> map = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return map;
        }
        List<ProductRatingVO> rows = reviewMapper.batchRatings(new ArrayList<>(productIds));
        if (rows != null) {
            for (ProductRatingVO row : rows) {
                map.put(row.getProductId(), row);
            }
        }
        return map;
    }

    @Override
    public ProductRatingVO rating(Long productId) {
        // 商品详情页与评价区都会取评分汇总，缓存 5 分钟；有任何评价变动会立即清除
        return cacheService.remember(CacheKeys.reviewSummary(productId), CacheKeys.TTL_REVIEW_SUMMARY,
                new TypeReference<ProductRatingVO>() {
                }, () -> reviewMapper.summary(productId));
    }

    @Override
    @Transactional
    public ProductReviewVO create(Long userId, ReviewCreateRequest request) {
        OrderItem item = orderItemMapper.selectById(request.getOrderItemId());
        if (item == null) {
            throw new BusinessException(ErrorCode.REVIEW_ITEM_NOT_FOUND);
        }
        Orders order = ordersMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_FINISHED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getOrderItemId, item.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        ProductReview review = new ProductReview();
        review.setOrderId(order.getId());
        review.setOrderItemId(item.getId());
        review.setProductId(item.getProductId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(normalizeContent(request.getContent()));
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        review.setStatus(1);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        evictProductRating(review.getProductId());
        return toVO(reviewMapper.selectById(review.getId()));
    }

    @Override
    public ProductReviewPageVO pageByProduct(Long productId, int pageNum, int pageSize, Integer rating) {
        int num = pageNum <= 0 ? 1 : pageNum;
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 50);
        IPage<ProductReviewVO> page = reviewMapper.pageByProduct(
                new Page<>(num, size), productId, rating);
        page.getRecords().forEach(this::maskNickname);

        ProductRatingVO summary = rating(productId);
        BigDecimal avg = summary == null || summary.getRating() == null
                ? BigDecimal.ZERO : summary.getRating();
        long total = summary == null || summary.getReviewCount() == null ? 0 : summary.getReviewCount();

        Map<Integer, Long> starDist = new HashMap<>();
        for (int star = 5; star >= 1; star--) {
            starDist.put(star, 0L);
        }
        List<StarCountVO> raw = reviewMapper.starDist(productId);
        if (raw != null) {
            for (StarCountVO row : raw) {
                if (row.getRating() != null) {
                    starDist.put(row.getRating(), row.getCnt() == null ? 0L : row.getCnt());
                }
            }
        }

        return ProductReviewPageVO.of(avg, total, starDist, PageResult.of(page));
    }

    @Override
    public List<ProductReviewVO> listByOrder(Long userId, Long orderId) {
        List<ProductReviewVO> list = reviewMapper.listByOrder(orderId, userId);
        list.forEach(this::maskNickname);
        return list;
    }

    @Override
    public PageResult<ProductReviewVO> pageAdmin(Long productId, Integer status, Integer rating,
                                                 String keyword, int pageNum, int pageSize) {
        int num = pageNum <= 0 ? 1 : pageNum;
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        IPage<ProductReviewVO> page = reviewMapper.pageAdmin(new Page<>(num, size),
                productId, status, rating, keyword == null ? null : keyword.trim());
        page.getRecords().forEach(this::maskNickname);
        return PageResult.of(page);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        ProductReview review = requireReview(id);
        review.setStatus(status);
        reviewMapper.updateById(review);
        evictProductRating(review.getProductId());
    }

    @Override
    @Transactional
    public ProductReviewVO reply(Long id, String reply) {
        ProductReview review = requireReview(id);
        String text = reply == null ? "" : reply.trim();
        if (text.isEmpty()) {
            review.setReply(null);
            review.setReplyTime(null);
        } else {
            review.setReply(text);
            review.setReplyTime(LocalDateTime.now());
        }
        reviewMapper.updateById(review);
        return toVO(reviewMapper.selectById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductReview review = requireReview(id);
        reviewMapper.deleteById(id);
        evictProductRating(review.getProductId());
    }

    /** 评价变化会影响评分汇总、商品详情与商品列表里的星级，一并清掉 */
    private void evictProductRating(Long productId) {
        if (productId == null) {
            return;
        }
        cacheService.evict(CacheKeys.reviewSummary(productId), CacheKeys.productDetail(productId));
        cacheService.evictByPrefix(CacheKeys.PRODUCT_PAGE);
    }

    private ProductReview requireReview(Long id) {
        ProductReview review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String text = content.trim();
        return text.isEmpty() ? null : text;
    }

    /** 匿名评价不暴露昵称 */
    private void maskNickname(ProductReviewVO vo) {
        if (vo == null) {
            return;
        }
        if (vo.getAnonymous() != null && vo.getAnonymous() == 1) {
            vo.setNickname("匿名用户");
            return;
        }
        if (vo.getNickname() == null || vo.getNickname().isBlank()) {
            vo.setNickname("会员用户");
        }
    }

    private ProductReviewVO toVO(ProductReview review) {
        ProductReviewVO vo = new ProductReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setOrderItemId(review.getOrderItemId());
        vo.setProductId(review.getProductId());
        vo.setUserId(review.getUserId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setAnonymous(review.getAnonymous());
        vo.setStatus(review.getStatus());
        vo.setReply(review.getReply());
        vo.setReplyTime(review.getReplyTime());
        vo.setCreateTime(review.getCreateTime());
        maskNickname(vo);
        return vo;
    }
}
