package com.shoping.module.notice.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.PageResult;
import com.shoping.common.result.Result;
import com.shoping.module.notice.entity.UserNotice;
import com.shoping.module.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站内消息（取餐提醒）：未读数驱动前端红点
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", noticeService.unreadCount(AuthContext.requireUserId())));
    }

    @GetMapping("/list")
    public Result<PageResult<UserNotice>> list(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(noticeService.page(AuthContext.requireUserId(), pageNum, pageSize));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        noticeService.markRead(AuthContext.requireUserId(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        noticeService.markAllRead(AuthContext.requireUserId());
        return Result.ok();
    }

    /** 进入订单详情时把该订单的提醒标记已读 */
    @PutMapping("/order/{orderId}/read")
    public Result<Void> readOrder(@PathVariable Long orderId) {
        noticeService.markOrderRead(AuthContext.requireUserId(), orderId);
        return Result.ok();
    }
}
