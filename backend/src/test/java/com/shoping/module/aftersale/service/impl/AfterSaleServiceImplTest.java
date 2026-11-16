package com.shoping.module.aftersale.service.impl;

import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.aftersale.dto.AfterSaleProcessRequest;
import com.shoping.module.aftersale.entity.AfterSale;
import com.shoping.module.aftersale.mapper.AfterSaleMapper;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrderItemMapper;
import com.shoping.module.order.mapper.OrderQueueMapper;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.stock.service.StockRollbackService;
import com.shoping.module.user.service.BalanceService;
import com.shoping.module.user.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 售后退款金额校验单元测试。
 * <p>
 * 覆盖两组：
 * 1. 私有纯函数（normalizeRefund / validateRefund / balanceRefundable）——反射直接调用，精确测边界；
 * 2. process 公共入口——mock 协作者，验证超额退款在入口就被拦截、不会走到退款/改单。
 */
@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {

    @Mock private AfterSaleMapper afterSaleMapper;
    @Mock private OrdersMapper ordersMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderQueueMapper orderQueueMapper;
    @Mock private BalanceService balanceService;
    @Mock private MemberService memberService;
    @Mock private StockRollbackService stockRollbackService;

    private AfterSaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AfterSaleServiceImpl(afterSaleMapper, ordersMapper, orderItemMapper,
                orderQueueMapper, balanceService, memberService, stockRollbackService);
    }

    // ============ 反射调用私有纯函数 ============

    private Object invokePrivate(String name, Class<?>[] types, Object... args) {
        try {
            Method m = AfterSaleServiceImpl.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m.invoke(service, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // 反射把「方法内部抛出的异常」包在 InvocationTargetException 里，
            // 必须把原始 BusinessException 抛出去，断言才能识别错误码
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal normalize(BigDecimal raw) {
        return (BigDecimal) invokePrivate("normalizeRefund",
                new Class<?>[]{BigDecimal.class}, raw);
    }

    private void validate(Orders order, BigDecimal refund, BigDecimal already) {
        invokePrivate("validateRefund",
                new Class<?>[]{Orders.class, BigDecimal.class, BigDecimal.class},
                order, refund, already);
    }

    private BigDecimal balanceRefundable(Orders order, BigDecimal already, BigDecimal refund) {
        return (BigDecimal) invokePrivate("balanceRefundable",
                new Class<?>[]{Orders.class, BigDecimal.class, BigDecimal.class},
                order, already, refund);
    }

    private Orders orderWith(String payAmount, String balancePaid) {
        Orders o = new Orders();
        o.setId(1L);
        o.setUserId(100L);
        o.setPayAmount(new BigDecimal(payAmount));
        o.setBalancePaid(new BigDecimal(balancePaid));
        return o;
    }

    @Nested
    @DisplayName("normalizeRefund 金额归一化")
    class Normalize {

        @Test
        @DisplayName("null 按 0 处理")
        void nullIsZero() {
            assertThat(normalize(null)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("负数被拒绝，错误码 13002")
        void negativeRejected() {
            assertThatThrownBy(() -> normalize(new BigDecimal("-0.01")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ErrorCode.AFTER_SALE_REFUND_INVALID.getCode()));
        }

        @Test
        @DisplayName("超过两位小数被拒绝")
        void tooManyDecimalsRejected() {
            assertThatThrownBy(() -> normalize(new BigDecimal("1.234")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("一位小数补齐为两位")
        void scaleNormalized() {
            assertThat(normalize(new BigDecimal("9.9")).toPlainString()).isEqualTo("9.90");
        }
    }

    @Nested
    @DisplayName("validateRefund 退款上限（实付 - 已退）")
    class Validate {

        @Test
        @DisplayName("退款额恰等于可退上限时通过")
        void atLimitPasses() {
            Orders o = orderWith("100.00", "0.00");
            validate(o, new BigDecimal("100.00"), BigDecimal.ZERO);
        }

        @Test
        @DisplayName("超过上限 0.01 元即被拒，错误码 13003")
        void overLimitRejected() {
            Orders o = orderWith("100.00", "0.00");
            assertThatThrownBy(() -> validate(o, new BigDecimal("100.01"), BigDecimal.ZERO))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ErrorCode.AFTER_SALE_REFUND_EXCEED.getCode()));
        }

        @Test
        @DisplayName("已退 30 元后，上限收紧到 70 元")
        void alreadyRefundedReducesLimit() {
            Orders o = orderWith("100.00", "0.00");
            validate(o, new BigDecimal("70.00"), new BigDecimal("30.00"));
            assertThatThrownBy(() -> validate(o, new BigDecimal("70.01"), new BigDecimal("30.00")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("已退金额超过实付时，可退上限为 0，任何正退款被拒")
        void overRefundedClampsToZero() {
            Orders o = orderWith("50.00", "0.00");
            assertThatThrownBy(() -> validate(o, new BigDecimal("0.01"), new BigDecimal("60.00")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("0 元退款（仅退货/补发）直接放行")
        void zeroRefundPasses() {
            validate(orderWith("100.00", "0.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Test
        @DisplayName("订单不存在时拒绝退款")
        void nullOrderRejected() {
            assertThatThrownBy(() -> validate(null, new BigDecimal("1.00"), BigDecimal.ZERO))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("balanceRefundable 储值可退部分")
    class BalanceRefundable {

        @Test
        @DisplayName("未退过时：退款额不超过储值实付")
        void cappedByBalancePaid() {
            Orders o = orderWith("100.00", "50.00");
            assertThat(balanceRefundable(o, BigDecimal.ZERO, new BigDecimal("30.00")))
                    .isEqualByComparingTo("30.00");
            assertThat(balanceRefundable(o, BigDecimal.ZERO, new BigDecimal("80.00")))
                    .isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("已退部分占用储值额度后，剩余可退减少")
        void alreadyRefundedConsumesRoom() {
            Orders o = orderWith("100.00", "50.00");
            assertThat(balanceRefundable(o, new BigDecimal("40.00"), new BigDecimal("30.00")))
                    .isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("现金订单（储值实付 0）不产生储值退款")
        void cashOrderReturnsZero() {
            Orders o = orderWith("100.00", "0.00");
            assertThat(balanceRefundable(o, BigDecimal.ZERO, new BigDecimal("30.00")))
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("已退储值超额时兜底为 0，不出现负数")
        void neverNegative() {
            Orders o = orderWith("100.00", "50.00");
            assertThat(balanceRefundable(o, new BigDecimal("80.00"), new BigDecimal("30.00")))
                    .isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("process 入口：资损拦截在落库前生效")
    class ProcessGuard {

        private AfterSale pendingTicket;

        @BeforeEach
        void stubTicket() {
            pendingTicket = new AfterSale();
            pendingTicket.setId(10L);
            pendingTicket.setOrderId(1L);
            pendingTicket.setUserId(100L);
            pendingTicket.setType(1);
            pendingTicket.setStatus(0);
            when(afterSaleMapper.selectById(10L)).thenReturn(pendingTicket);
        }

        private AfterSaleProcessRequest request(BigDecimal refund) {
            AfterSaleProcessRequest r = new AfterSaleProcessRequest();
            r.setId(10L);
            r.setStatus(2);
            r.setRefundAmount(refund);
            return r;
        }

        @Test
        @DisplayName("超额退款：抛异常，且不调用退款/积分/库存任何副作用")
        void overRefundBlocksAllSideEffects() {
            when(ordersMapper.selectById(1L)).thenReturn(orderWith("50.00", "50.00"));
            when(afterSaleMapper.selectList(any())).thenReturn(List.of());

            assertThatThrownBy(() -> service.process(request(new BigDecimal("50.01"))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ErrorCode.AFTER_SALE_REFUND_EXCEED.getCode()));

            verify(balanceService, never()).refund(any(), any(), any());
            verify(memberService, never()).onRefund(any(), any(), any());
            verify(stockRollbackService, never()).rollbackSkuStock(any(), any());
            verify(afterSaleMapper, never()).updateById(any(AfterSale.class));
        }

        @Test
        @DisplayName("负数退款同样在入口被拦截")
        void negativeRefundBlocked() {
            when(ordersMapper.selectById(1L)).thenReturn(orderWith("50.00", "50.00"));

            assertThatThrownBy(() -> service.process(request(new BigDecimal("-1"))))
                    .isInstanceOf(BusinessException.class);

            verify(afterSaleMapper, never()).updateById(any(AfterSale.class));
        }
    }
}
