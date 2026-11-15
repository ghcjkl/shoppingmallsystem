package com.shoping.module.order.task;

import com.shoping.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待付款订单超时释放：
 * 下单时会锁定 SKU 库存，用户 15 分钟不付款就自动取消并回滚库存，避免库存被白白占用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 未支付超时分钟数 */
    private static final int TIMEOUT_MINUTES = 15;

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void releaseTimeoutOrders() {
        try {
            int cancelled = orderService.cancelTimeoutOrders(TIMEOUT_MINUTES);
            if (cancelled > 0) {
                log.info("自动取消超时未支付订单 {} 笔，库存已回滚", cancelled);
            }
        } catch (Exception e) {
            log.warn("超时订单扫描失败: {}", e.getMessage());
        }
    }
}
