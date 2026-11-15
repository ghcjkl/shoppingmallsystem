package com.shoping.module.notice.service;

import com.shoping.common.result.PageResult;
import com.shoping.module.notice.entity.UserNotice;
import com.shoping.module.order.entity.Orders;

public interface NoticeService {

    /** 订单进入「可取餐」状态时推送取餐提醒（同一订单只推一次） */
    void pushPickupReady(Orders order);

    /** 未读条数（前端红点） */
    long unreadCount(Long userId);

    PageResult<UserNotice> page(Long userId, int pageNum, int pageSize);

    /** 标记单条已读 */
    void markRead(Long userId, Long id);

    /** 全部标记已读 */
    void markAllRead(Long userId);

    /** 进入某订单时把该订单的提醒标记已读 */
    void markOrderRead(Long userId, Long orderId);
}
