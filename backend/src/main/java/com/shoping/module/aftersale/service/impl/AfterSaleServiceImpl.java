package com.shoping.module.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.aftersale.dto.AfterSaleApplyRequest;
import com.shoping.module.aftersale.dto.AfterSaleProcessRequest;
import com.shoping.module.aftersale.entity.AfterSale;
import com.shoping.module.aftersale.mapper.AfterSaleMapper;
import com.shoping.module.aftersale.service.AfterSaleService;
import com.shoping.module.order.entity.OrderItem;
import com.shoping.module.order.entity.OrderQueue;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrderItemMapper;
import com.shoping.module.order.mapper.OrderQueueMapper;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.stock.service.StockRollbackService;
import com.shoping.module.user.service.BalanceService;
import com.shoping.module.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    /** 订单状态：1 待付款 2 已支付 3 制作中 4 待取餐 5 已完成 6 已取消 7 售后中 */
    private static final int ORDER_PENDING_PAY = 1;
    private static final int ORDER_PAID = 2;
    private static final int ORDER_MAKING = 3;
    private static final int ORDER_READY = 4;
    private static final int ORDER_FINISHED = 5;
    private static final int ORDER_CANCELLED = 6;
    private static final int ORDER_AFTER_SALE = 7;

    /** 排队状态 */
    private static final int QUEUE_MAKING = 2;
    private static final int QUEUE_READY = 3;
    private static final int QUEUE_FINISHED = 4;
    private static final int QUEUE_CANCELLED = 5;

    /** 订单类型 */
    private static final int ORDER_TYPE_STANDARD = 1;
    private static final int ORDER_TYPE_FRESH = 2;

    /** 售后状态：0 待处理 1 处理中 2 已完成 3 已拒绝 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_REJECTED = 3;

    /** 售后类型：1 退款 2 退货退款（涉及库存） */
    private static final int TYPE_REFUND = 1;
    private static final int TYPE_RETURN_REFUND = 2;

    private final AfterSaleMapper afterSaleMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderQueueMapper orderQueueMapper;
    private final BalanceService balanceService;
    private final MemberService memberService;
    private final StockRollbackService stockRollbackService;

    @Override
    @Transactional
    public AfterSale apply(AfterSaleApplyRequest request) {
        Orders order = ordersMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(request.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        AfterSale afterSale = new AfterSale();
        afterSale.setOrderId(request.getOrderId());
        afterSale.setOrderItemId(request.getOrderItemId());
        // 记下申请前的订单状态：拒绝或售后结束后据此恢复，避免订单永远卡在「售后中」
        afterSale.setPrevOrderStatus(order.getStatus());
        afterSale.setUserId(request.getUserId());
        afterSale.setType(request.getType());
        afterSale.setReason(request.getReason());
        afterSale.setImageUrls(request.getImageUrls());
        afterSale.setStatus(STATUS_PENDING);
        afterSaleMapper.insert(afterSale);

        order.setStatus(ORDER_AFTER_SALE);
        ordersMapper.updateById(order);
        return afterSale;
    }

    @Override
    public List<AfterSale> listByUser(Long userId) {
        return afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .orderByDesc(AfterSale::getId));
    }

    @Override
    public List<AfterSale> listAll(Integer status) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AfterSale::getStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getId);
        return afterSaleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public AfterSale process(AfterSaleProcessRequest request) {
        AfterSale afterSale = afterSaleMapper.selectById(request.getId());
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        }
        int originalStatus = afterSale.getStatus() == null ? STATUS_PENDING : afterSale.getStatus();
        int targetStatus = request.getStatus() == null ? originalStatus : request.getStatus();
        boolean firstDone = targetStatus == STATUS_DONE && originalStatus != STATUS_DONE;
        boolean firstRejected = targetStatus == STATUS_REJECTED && originalStatus != STATUS_REJECTED;

        Orders order = ordersMapper.selectById(afterSale.getOrderId());
        // 已退金额要排除本单：此时本单尚未落库新状态，避免把本次重复计入
        BigDecimal alreadyRefunded = order == null
                ? BigDecimal.ZERO
                : sumCompletedRefund(order.getId(), afterSale.getId());
        BigDecimal refund = normalizeRefund(request.getRefundAmount());

        if (targetStatus == STATUS_DONE) {
            // 已完成工单不允许再改金额，否则等于篡改财务记录
            if (originalStatus == STATUS_DONE && afterSale.getRefundAmount() != null
                    && refund.compareTo(afterSale.getRefundAmount()) != 0) {
                throw new BusinessException(ErrorCode.AFTER_SALE_REFUND_INVALID,
                        "工单已完成，不能再修改退款金额");
            }
            validateRefund(order, refund, alreadyRefunded);
        } else {
            // 处理中 / 拒绝：不保留金额，避免「拒绝了却挂着退款额」
            refund = null;
        }

        afterSale.setStatus(targetStatus);
        afterSale.setRefundAmount(refund);
        afterSale.setResult(request.getResult());
        afterSaleMapper.updateById(afterSale);

        // 处理中(1) 或重复提交：不改订单与库存
        if (!firstDone && !firstRejected) {
            return afterSale;
        }

        if (firstRejected) {
            // 拒绝：订单回到申请前的状态，继续履约
            restoreOrderStatus(order, afterSale);
            return afterSale;
        }

        // 完成：先退库存、扣积分、退储值，再决定订单终态
        if (order != null && needRestock(afterSale, request)) {
            restock(afterSale, order);
        }
        // 若该订单用了积分抵扣：把下单时锁定的积分原额退回（与 onRefund 扣回的奖励积分互不影响）
        if (order != null && order.getPointsDeducted() != null && order.getPointsDeducted() > 0) {
            memberService.refundPoints(afterSale.getUserId(), afterSale.getOrderId(), order.getPointsDeducted());
        }
        if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0 && order != null) {
            // 退款金额对应的会员积分 / 成长值一并扣回
            memberService.onRefund(afterSale.getUserId(), afterSale.getOrderId(), refund);
            // 储值支付部分退回本金（现金退款由线下/第三方处理）
            BigDecimal balanceRefund = balanceRefundable(order, alreadyRefunded, refund);
            if (balanceRefund.compareTo(BigDecimal.ZERO) > 0) {
                balanceService.refund(afterSale.getUserId(), afterSale.getOrderId(), balanceRefund);
            }
        }
        settleOrder(order, afterSale);
        return afterSale;
    }

    /** 金额归一化：空按 0，负数拒绝，统一保留两位小数 */
    private BigDecimal normalizeRefund(BigDecimal raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.AFTER_SALE_REFUND_INVALID, "退款金额不能为负数");
        }
        if (raw.scale() > 2) {
            throw new BusinessException(ErrorCode.AFTER_SALE_REFUND_INVALID, "退款金额最多两位小数");
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 退款额度校验：本次金额不得超过「订单实付 - 该订单已退」。
     * 这是防超额退款的核心校验，管理端填多少都要过这里。
     */
    private void validateRefund(Orders order, BigDecimal refund, BigDecimal alreadyRefunded) {
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (refund.compareTo(BigDecimal.ZERO) == 0) {
            // 不退钱（例如只退货、补发、仅退款 0 元）走正常流程
            return;
        }
        BigDecimal paid = nvlMoney(order.getPayAmount());
        BigDecimal available = paid.subtract(alreadyRefunded).max(BigDecimal.ZERO);
        if (refund.compareTo(available) > 0) {
            throw new BusinessException(ErrorCode.AFTER_SALE_REFUND_EXCEED,
                    String.format("退款金额不能超过可退上限 ¥%s（订单实付 ¥%s，已退 ¥%s）",
                            available.toPlainString(), paid.toPlainString(),
                            alreadyRefunded.toPlainString()));
        }
    }

    /**
     * 储值可退部分：不超过该订单储值实付，且不超过本单退款额。
     * 系统语义是「退款优先抵储值」，所以已退储值 = min(已退合计, 储值实付)。
     */
    private BigDecimal balanceRefundable(Orders order, BigDecimal alreadyRefunded, BigDecimal refund) {
        BigDecimal balanceCap = nvlMoney(order.getBalancePaid());
        if (balanceCap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal balanceUsed = alreadyRefunded.min(balanceCap);
        BigDecimal room = balanceCap.subtract(balanceUsed).max(BigDecimal.ZERO);
        return refund.min(room);
    }

    private BigDecimal nvlMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 是否需要回滚库存：仅「退款 / 退货退款」涉及，补发、补偿不动库存；
     * 管理员可在处理时手动取消勾选（restock=false），例如商品已被顾客丢弃。
     */
    private boolean needRestock(AfterSale afterSale, AfterSaleProcessRequest request) {
        Integer type = afterSale.getType();
        if (type == null || (type != TYPE_REFUND && type != TYPE_RETURN_REFUND)) {
            return false;
        }
        return !Boolean.FALSE.equals(request.getRestock());
    }

    private void restock(AfterSale afterSale, Orders order) {
        // 订单已取消（下单或取消时已回补过库存），避免重复加库存
        if (order.getStatus() != null && order.getStatus() == ORDER_CANCELLED) {
            log.warn("售后退库存跳过：订单已取消 orderId={}, afterSaleId={}", order.getId(), afterSale.getId());
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        if (afterSale.getOrderItemId() != null) {
            items = items.stream()
                    .filter(i -> afterSale.getOrderItemId().equals(i.getId()))
                    .toList();
        }
        if (items.isEmpty()) {
            log.warn("售后退库存跳过：未找到订单明细 orderId={}, afterSaleId={}", order.getId(), afterSale.getId());
            return;
        }
        String action = afterSale.getType() == TYPE_RETURN_REFUND ? "退货回滚" : "退款回滚";
        String reason = "售后 #" + afterSale.getId() + "（订单 " + order.getOrderNo() + "）" + action;

        int skuQty = stockRollbackService.rollbackSkuStock(items, reason);
        if (order.getOrderType() != null && order.getOrderType() == ORDER_TYPE_FRESH) {
            stockRollbackService.rollbackMaterials(items, reason);
        }
        log.info("售后退库存完成：afterSaleId={}, orderId={}, 商品回补={}件", afterSale.getId(), order.getId(), skuQty);
    }

    /**
     * 售后完成后的订单终态：
     * 累计退款已覆盖实付（全额退款）→ 交易关闭(6)；否则恢复申请前的状态继续履约。
     */
    private void settleOrder(Orders order, AfterSale afterSale) {
        if (order == null) {
            return;
        }
        BigDecimal paid = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        BigDecimal refunded = sumCompletedRefund(order.getId());
        if (paid.compareTo(BigDecimal.ZERO) > 0 && refunded.compareTo(paid) >= 0) {
            if (order.getStatus() == null || order.getStatus() != ORDER_CANCELLED) {
                order.setStatus(ORDER_CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                ordersMapper.updateById(order);
                cancelOpenQueue(order.getId());
                log.info("售后全额退款完成，订单关闭：orderId={}, 退款={}", order.getId(), refunded);
            }
            return;
        }
        restoreOrderStatus(order, afterSale);
    }

    /** 把订单从「售后中」恢复到申请前的状态（仅当订单仍处于售后中时才改，避免覆盖其它流程） */
    private void restoreOrderStatus(Orders order, AfterSale afterSale) {
        if (order == null || order.getStatus() == null || order.getStatus() != ORDER_AFTER_SALE) {
            return;
        }
        int target = resolvePreviousStatus(order, afterSale);
        if (target == ORDER_FINISHED && order.getFinishTime() == null) {
            order.setFinishTime(LocalDateTime.now());
        }
        order.setStatus(target);
        ordersMapper.updateById(order);
        log.info("售后结束，订单状态恢复：orderId={}, afterSaleId={}, {} -> {}",
                order.getId(), afterSale.getId(), ORDER_AFTER_SALE, target);
    }

    /** 前置状态优先取工单记录；老工单没有记录时按支付/完成时间与排队情况推断 */
    private int resolvePreviousStatus(Orders order, AfterSale afterSale) {
        Integer prev = afterSale.getPrevOrderStatus();
        if (prev != null && prev != ORDER_AFTER_SALE && prev != ORDER_CANCELLED) {
            return prev;
        }
        if (order.getFinishTime() != null) {
            return ORDER_FINISHED;
        }
        if (order.getPayTime() == null) {
            return ORDER_PENDING_PAY;
        }
        OrderQueue queue = findQueue(order.getId());
        if (queue != null && queue.getStatus() != null) {
            return switch (queue.getStatus()) {
                case QUEUE_FINISHED -> ORDER_FINISHED;
                case QUEUE_READY -> ORDER_READY;
                case QUEUE_MAKING -> ORDER_MAKING;
                case QUEUE_CANCELLED -> ORDER_CANCELLED;
                default -> ORDER_PAID;
            };
        }
        return order.getOrderType() != null && order.getOrderType() == ORDER_TYPE_STANDARD
                ? ORDER_READY
                : ORDER_PAID;
    }

    /** 关闭未取餐/未完成的排队记录 */
    private void cancelOpenQueue(Long orderId) {
        OrderQueue queue = findQueue(orderId);
        if (queue == null || queue.getStatus() == null) {
            return;
        }
        if (queue.getStatus() == QUEUE_FINISHED || queue.getStatus() == QUEUE_CANCELLED) {
            return;
        }
        queue.setStatus(QUEUE_CANCELLED);
        orderQueueMapper.updateById(queue);
    }

    /** 该订单已完成售后的退款合计 */
    private BigDecimal sumCompletedRefund(Long orderId) {
        return sumCompletedRefund(orderId, null);
    }

    /** 汇总某订单已完成工单的退款额；excludeId 用于排除当前这一单（校验「本次之前已退多少」） */
    private BigDecimal sumCompletedRefund(Long orderId, Long excludeId) {
        List<AfterSale> list = afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, orderId)
                .eq(AfterSale::getStatus, STATUS_DONE)
                .ne(excludeId != null, AfterSale::getId, excludeId));
        return list.stream()
                .map(AfterSale::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderQueue findQueue(Long orderId) {
        return orderQueueMapper.selectOne(new LambdaQueryWrapper<OrderQueue>()
                .eq(OrderQueue::getOrderId, orderId));
    }
}
