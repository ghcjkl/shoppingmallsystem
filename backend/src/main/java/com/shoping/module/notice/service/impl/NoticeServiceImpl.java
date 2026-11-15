package com.shoping.module.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shoping.common.result.PageResult;
import com.shoping.module.notice.entity.UserNotice;
import com.shoping.module.notice.mapper.UserNoticeMapper;
import com.shoping.module.notice.service.NoticeService;
import com.shoping.module.order.entity.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    /** 1 取餐提醒 */
    private static final int TYPE_PICKUP = 1;

    private final UserNoticeMapper noticeMapper;

    @Override
    @Transactional
    public void pushPickupReady(Orders order) {
        if (order == null || order.getId() == null || order.getUserId() == null) {
            return;
        }
        Long exists = noticeMapper.selectCount(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, order.getUserId())
                .eq(UserNotice::getOrderId, order.getId())
                .eq(UserNotice::getType, TYPE_PICKUP));
        if (exists != null && exists > 0) {
            // 同一订单只提醒一次，避免状态来回切换时刷屏
            return;
        }

        boolean fresh = order.getOrderType() != null && order.getOrderType() == 2;
        String code = order.getPickupCode();
        StringBuilder content = new StringBuilder();
        if (code != null && !code.isBlank()) {
            content.append("取餐码 ").append(code);
        }
        if (order.getQueueNo() != null) {
            content.append(content.length() > 0 ? " · " : "").append("排队号 #").append(order.getQueueNo());
        }
        content.append(content.length() > 0 ? "，到店出示取餐码即可" : "订单已可取餐，请到店领取");

        UserNotice notice = new UserNotice();
        notice.setUserId(order.getUserId());
        notice.setType(TYPE_PICKUP);
        notice.setTitle(fresh ? "饮品已做好，请取餐" : "已备货，请取餐");
        notice.setContent(content.toString());
        notice.setOrderId(order.getId());
        notice.setReadFlag(0);
        noticeMapper.insert(notice);
    }

    @Override
    public long unreadCount(Long userId) {
        Long count = noticeMapper.selectCount(new LambdaQueryWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getReadFlag, 0));
        return count == null ? 0 : count;
    }

    @Override
    public PageResult<UserNotice> page(Long userId, int pageNum, int pageSize) {
        int num = pageNum <= 0 ? 1 : pageNum;
        int size = pageSize <= 0 ? 20 : Math.min(pageSize, 50);
        IPage<UserNotice> page = noticeMapper.selectPage(new Page<>(num, size),
                new LambdaQueryWrapper<UserNotice>()
                        .eq(UserNotice::getUserId, userId)
                        .orderByDesc(UserNotice::getId));
        return PageResult.of(page);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long id) {
        noticeMapper.update(null, new LambdaUpdateWrapper<UserNotice>()
                .eq(UserNotice::getId, id)
                .eq(UserNotice::getUserId, userId)
                .set(UserNotice::getReadFlag, 1));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        noticeMapper.update(null, new LambdaUpdateWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getReadFlag, 0)
                .set(UserNotice::getReadFlag, 1));
    }

    @Override
    @Transactional
    public void markOrderRead(Long userId, Long orderId) {
        noticeMapper.update(null, new LambdaUpdateWrapper<UserNotice>()
                .eq(UserNotice::getUserId, userId)
                .eq(UserNotice::getOrderId, orderId)
                .eq(UserNotice::getReadFlag, 0)
                .set(UserNotice::getReadFlag, 1));
    }
}
