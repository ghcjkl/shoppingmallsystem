package com.shoping.module.admin.controller;

import com.shoping.common.result.PageResult;
import com.shoping.common.result.Result;
import com.shoping.module.review.dto.ReviewReplyRequest;
import com.shoping.module.review.service.ReviewService;
import com.shoping.module.review.vo.ProductReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 商品评价管理（查看 / 隐藏 / 回复 / 删除）
 */
@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<PageResult<ProductReviewVO>> page(@RequestParam(required = false) Long productId,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Integer rating,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(reviewService.pageAdmin(productId, status, rating, keyword, pageNum, pageSize));
    }

    /** status：1 显示 / 0 隐藏 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        reviewService.updateStatus(id, status);
        return Result.ok();
    }

    /** 商家回复，传空串清除回复 */
    @PutMapping("/{id}/reply")
    public Result<ProductReviewVO> reply(@PathVariable Long id,
                                         @Valid @RequestBody ReviewReplyRequest request) {
        return Result.ok(reviewService.reply(id, request.getReply()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.ok();
    }
}
