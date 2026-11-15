package com.shoping.module.order.service;

import com.shoping.module.order.dto.OrderCreateRequest;
import com.shoping.module.order.vo.OrderVO;
import com.shoping.module.order.vo.QueueVO;

import java.util.List;

public interface OrderService {

    OrderVO create(OrderCreateRequest request);

    /**
     * 支付订单
     *
     * @param operatorId 当前登录用户（管理员操作时传管理员 ID，校验时按 admin 放行）
     * @param admin      是否管理员；否则只允许支付自己的订单，避免替他人扣款
     */
    OrderVO pay(Long orderId, Integer payType, Long operatorId, boolean admin);

    OrderVO cancel(Long orderId, Long userId);

    /**
     * 订单详情
     *
     * @param viewerId 当前登录用户
     * @param admin    是否管理员；顾客只能看自己的订单，防止猜订单号窥探他人信息
     */
    OrderVO detail(Long orderId, Long viewerId, boolean admin);

    List<OrderVO> listByUser(Long userId);

    /** 门店维度订单列表（管理端：按状态/订单类型筛选，如标品待取餐） */
    List<OrderVO> listByStore(Long storeId, Integer status, Integer orderType);

    List<QueueVO> queueList(Long storeId);

    void startMaking(Long orderId);

    void finishMaking(Long orderId);

    OrderVO complete(Long orderId);

    OrderVO verifyPickup(Long storeId, String pickupCode);

    /** 自动取消超过 minutes 分钟仍未支付的订单，释放已锁库存；返回处理笔数 */
    int cancelTimeoutOrders(int minutes);
}
