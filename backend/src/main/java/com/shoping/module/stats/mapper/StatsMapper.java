package com.shoping.module.stats.mapper;

import com.shoping.module.stats.vo.CategoryStatVO;
import com.shoping.module.stats.vo.HourStatVO;
import com.shoping.module.stats.vo.OrderTypeStatVO;
import com.shoping.module.stats.vo.PayTypeStatVO;
import com.shoping.module.stats.vo.PeriodStatVO;
import com.shoping.module.stats.vo.ProductSalesVO;
import com.shoping.module.stats.vo.StatusCountVO;
import com.shoping.module.stats.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 销售统计聚合查询（只读）
 * <p>
 * 统计口径：已完成支付的订单（status ∈ 2,3,4,5,7）按 pay_time 归集，
 * 待付款与已取消订单不计入；已完成全额售后退款的订单整单排除，
 * 部分退款的订单按「实付 − 已退款」计入净销售额。
 */
@Mapper
public interface StatsMapper {

    /** 订单已退款金额（已完成售后 status=2）：主表别名 orders */
    String REFUNDED = "(SELECT COALESCE(SUM(a.refund_amount), 0) FROM after_sale a "
            + "WHERE a.order_id = orders.id AND a.status = 2)";

    /** 同上，订单别名 o（联表查询用） */
    String REFUNDED_O = "(SELECT COALESCE(SUM(a.refund_amount), 0) FROM after_sale a "
            + "WHERE a.order_id = o.id AND a.status = 2)";

    String PAID = " FROM orders WHERE deleted = 0 AND pay_time IS NOT NULL "
            + "AND status IN (2,3,4,5,7) AND store_id = #{storeId} "
            + "AND pay_time >= #{start} AND pay_time < #{end} "
            + "AND " + REFUNDED + " < pay_amount ";

    /** 净销售额：实付金额扣除已退款部分 */
    String NET_SALES = "COALESCE(SUM(pay_amount), 0) - COALESCE(SUM(" + REFUNDED + "), 0)";

    @Select("SELECT DATE(pay_time) AS stat_date, COUNT(*) AS orders, "
            + NET_SALES + " AS sales"
            + PAID + "GROUP BY DATE(pay_time) ORDER BY stat_date")
    List<TrendPointVO> dailyTrend(@Param("storeId") Long storeId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Select("SELECT " + NET_SALES + " AS sales, COUNT(*) AS orders" + PAID)
    PeriodStatVO periodStat(@Param("storeId") Long storeId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0 AND store_id = #{storeId} "
            + "AND create_time >= #{start} AND create_time < #{end}")
    long createdOrders(@Param("storeId") Long storeId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);

    @Select("SELECT oi.product_name AS name, SUM(oi.quantity) AS qty, SUM(oi.amount) AS amount "
            + "FROM order_item oi JOIN orders o ON o.id = oi.order_id "
            + "WHERE o.deleted = 0 AND o.status IN (2,3,4,5,7) AND o.store_id = #{storeId} "
            + "AND o.pay_time >= #{start} AND o.pay_time < #{end} "
            + "AND " + REFUNDED_O + " < o.pay_amount "
            + "GROUP BY oi.product_name ORDER BY qty DESC, amount DESC LIMIT 10")
    List<ProductSalesVO> topProducts(@Param("storeId") Long storeId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Select("SELECT pay_type AS payType, COUNT(*) AS orders, "
            + NET_SALES + " AS amount"
            + PAID + "GROUP BY pay_type ORDER BY amount DESC")
    List<PayTypeStatVO> payTypes(@Param("storeId") Long storeId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Select("SELECT order_type AS orderType, COUNT(*) AS orders, "
            + NET_SALES + " AS amount"
            + PAID + "GROUP BY order_type")
    List<OrderTypeStatVO> orderTypes(@Param("storeId") Long storeId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Select("SELECT status AS status, COUNT(*) AS orders FROM orders "
            + "WHERE deleted = 0 AND store_id = #{storeId} "
            + "AND create_time >= #{start} AND create_time < #{end} GROUP BY status ORDER BY status")
    List<StatusCountVO> statusDist(@Param("storeId") Long storeId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /** 24 小时营业分布：按支付时间取小时 */
    @Select("SELECT HOUR(pay_time) AS statHour, COUNT(*) AS orders, "
            + NET_SALES + " AS sales"
            + PAID + "GROUP BY HOUR(pay_time) ORDER BY statHour")
    List<HourStatVO> hourDist(@Param("storeId") Long storeId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** 区间内消费过的会员数（去重，排除全额退款订单） */
    @Select("SELECT COUNT(DISTINCT user_id) FROM orders "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL AND status IN (2,3,4,5,7) "
            + "AND store_id = #{storeId} AND pay_time >= #{start} AND pay_time < #{end} "
            + "AND " + REFUNDED + " < pay_amount")
    long periodBuyers(@Param("storeId") Long storeId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * 区间内首次消费的新客数：先取每个用户历史最早支付时间，再筛选落在区间内的。
     * 跨区间统计历史，保证「新客」不被时间窗口截断。
     */
    @Select("SELECT COUNT(*) FROM ("
            + "SELECT user_id, MIN(pay_time) AS first_pay FROM orders "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL AND status IN (2,3,4,5,7) "
            + "AND store_id = #{storeId} GROUP BY user_id"
            + ") t WHERE t.first_pay >= #{start} AND t.first_pay < #{end}")
    long periodNewBuyers(@Param("storeId") Long storeId,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    /** 毛销售额（退款前），退款率的分母 */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM orders "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL AND status IN (2,3,4,5,7) "
            + "AND store_id = #{storeId} AND pay_time >= #{start} AND pay_time < #{end}")
    BigDecimal grossSales(@Param("storeId") Long storeId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /** 区间内已完成售后（status=2）的退款金额，按申请时间归集 */
    @Select("SELECT COALESCE(SUM(a.refund_amount), 0) FROM after_sale a "
            + "JOIN orders o ON o.id = a.order_id "
            + "WHERE a.status = 2 AND o.deleted = 0 AND o.store_id = #{storeId} "
            + "AND a.create_time >= #{start} AND a.create_time < #{end}")
    BigDecimal periodRefundAmount(@Param("storeId") Long storeId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /** 区间内完成的售后工单数 */
    @Select("SELECT COUNT(*) FROM after_sale a JOIN orders o ON o.id = a.order_id "
            + "WHERE a.status = 2 AND o.deleted = 0 AND o.store_id = #{storeId} "
            + "AND a.create_time >= #{start} AND a.create_time < #{end}")
    long periodRefundOrders(@Param("storeId") Long storeId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    /** 分类销售结构：商品被删除时归入「未分类」 */
    @Select("SELECT c.id AS categoryId, c.name AS name, "
            + "SUM(oi.quantity) AS qty, SUM(oi.amount) AS amount "
            + "FROM order_item oi JOIN orders o ON o.id = oi.order_id "
            + "LEFT JOIN product p ON p.id = oi.product_id AND p.deleted = 0 "
            + "LEFT JOIN category c ON c.id = p.category_id "
            + "WHERE o.deleted = 0 AND o.status IN (2,3,4,5,7) AND o.store_id = #{storeId} "
            + "AND o.pay_time >= #{start} AND o.pay_time < #{end} "
            + "AND " + REFUNDED_O + " < o.pay_amount "
            + "GROUP BY c.id, c.name ORDER BY amount DESC")
    List<CategoryStatVO> categoryStats(@Param("storeId") Long storeId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
