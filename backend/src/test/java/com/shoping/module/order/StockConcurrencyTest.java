package com.shoping.module.order;

import com.shoping.common.exception.BusinessException;
import com.shoping.module.order.dto.OrderCreateRequest;
import com.shoping.module.order.dto.OrderItemRequest;
import com.shoping.module.order.entity.Orders;
import com.shoping.module.order.mapper.OrdersMapper;
import com.shoping.module.order.service.OrderService;
import com.shoping.module.product.entity.ProductSku;
import com.shoping.module.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发超卖测试：验证数据库条件更新（UPDATE ... WHERE stock >= qty）在多线程下不超卖。
 * <p>
 * 关键设计：本类【不能用类级 @Transactional】。Spring 测试事务绑定在主线程，
 * 子线程通过独立连接操作数据库，既读不到主线程的未提交数据，也不会被回滚；
 * 因此改用 @BeforeEach 记录基线、@AfterEach 恢复库存并删除测试订单的方式隔离。
 * <p>
 * 用 CountDownLatch 让所有线程同时起跑，放大竞争窗口，确保测到真并发而非串行。
 */
@SpringBootTest
@ActiveProfiles("test")
class StockConcurrencyTest {

    private static final Long SKU_ID = 1L;          // 原味薯片标品 SKU
    private static final Long PRODUCT_ID = 1L;
    private static final int THREADS = 20;

    @Autowired private ProductSkuMapper productSkuMapper;
    @Autowired private OrdersMapper ordersMapper;
    @Autowired private OrderService orderService;

    private int originalStock;
    private long baselineMaxOrderId;

    @BeforeEach
    void setUp() {
        ProductSku sku = productSkuMapper.selectById(SKU_ID);
        originalStock = sku.getStock() == null ? 0 : sku.getStock();
        // 记录测试前最大订单 id，AfterEach 只删新建的，不碰基线
        List<Orders> all = ordersMapper.selectList(null);
        baselineMaxOrderId = all.stream().mapToLong(Orders::getId).max().orElse(0L);
    }

    @AfterEach
    void tearDown() {
        // 恢复库存：用相对增量把当前值调回测试前的值（increaseStock 本身原子，不会丢失并发增量）
        productSkuMapper.increaseStock(SKU_ID, originalStock - currentStock());
        // 删除本次测试新建的订单（测试库无外键，按 id 条件删除即可）
        ordersMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Orders>()
                .gt(Orders::getId, baselineMaxOrderId));
    }

    private int currentStock() {
        Integer stock = productSkuMapper.selectById(SKU_ID).getStock();
        return stock == null ? 0 : stock;
    }

    private void setStock(int value) {
        productSkuMapper.increaseStock(SKU_ID, value - currentStock());
    }

    /** 并发执行 threads 个任务，用 CountDownLatch 保证同时起跑，等待全部完成 */
    private void runConcurrently(int threads, java.util.function.IntConsumer task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                task.accept(idx);
                return null;
            }));
        }
        start.countDown();                 // 所有人同时起跑
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);   // 等待全部完成，线程内异常在此抛出
        }
        pool.shutdownNow();
    }

    @Test
    @DisplayName("Mapper 层原子扣减：库存 10，20 线程各扣 1，恰好 10 成功、库存归零")
    void atomicDeductNoOversell() throws Exception {
        setStock(10);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        runConcurrently(THREADS, idx -> {
            int rows = productSkuMapper.deductStock(SKU_ID, 1);
            if (rows == 1) {
                success.incrementAndGet();
            } else {
                fail.incrementAndGet();
            }
        });

        assertThat(success.get()).isEqualTo(10);
        assertThat(fail.get()).isEqualTo(10);
        assertThat(currentStock()).isZero();
    }

    @Test
    @DisplayName("多件扣减：库存 10，每单扣 3，最多成功 3 单、库存余 1（不能扣成负数）")
    void multiQtyDeductNoOversell() throws Exception {
        setStock(10);
        AtomicInteger success = new AtomicInteger();

        runConcurrently(THREADS, idx -> {
            if (productSkuMapper.deductStock(SKU_ID, 3) == 1) {
                success.incrementAndGet();
            }
        });

        assertThat(success.get()).isEqualTo(3);
        assertThat(currentStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("完整下单链路并发：库存 8，20 线程同时下单，成功数 = 扣减数，库存不为负")
    void concurrentCreateOrderNoOversell() throws Exception {
        setStock(8);
        AtomicInteger orderCreated = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        runConcurrently(THREADS, idx -> {
            OrderCreateRequest req = new OrderCreateRequest();
            req.setUserId(2L);
            req.setStoreId(1L);
            req.setOrderType(1);
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(PRODUCT_ID);
            item.setSkuId(SKU_ID);
            item.setQuantity(1);
            req.setItems(List.of(item));
            try {
                orderService.create(req);
                orderCreated.incrementAndGet();
            } catch (BusinessException be) {
                // 库存不足（或偶发订单号碰撞）属于预期失败，不计为成功
            } catch (Exception e) {
                unexpected.add(e);
            }
        });

        assertThat(unexpected).isEmpty();
        int created = orderCreated.get();
        // 核心断言1：成功下单数不超过初始库存，绝不超卖
        assertThat(created).isLessThanOrEqualTo(8);
        assertThat(created).isPositive();
        // 核心断言2：剩余库存 = 初始 − 成功单数，每单恰好扣 1，不多扣也不少扣
        assertThat(currentStock()).isEqualTo(8 - created);
    }
}
