package com.shoping.module.stats.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售看板整体数据
 */
@Data
public class DashboardVO {

    /** 统计天数（7 / 14 / 30） */
    private int days;

    // ===== 今日 =====
    private BigDecimal todaySales;
    private BigDecimal todayAvgTicket;
    private long todayOrders;
    /** 今日新建订单数（含未支付/已取消） */
    private long todayCreated;

    // ===== 昨日（用于环比） =====
    private BigDecimal yesterdaySales;
    private long yesterdayOrders;
    /** 销售额环比（%），昨日为 0 时按 0 或 100 处理 */
    private double salesRate;
    private double ordersRate;

    // ===== 区间 =====
    private BigDecimal periodSales;
    private long periodOrders;

    // ===== 区间经营（会员 / 退款） =====
    /** 区间内消费过的会员数（去重） */
    private long periodBuyers;
    /** 区间内首次消费的新客数 */
    private long periodNewBuyers;
    /** 人均消费 = 净销售额 / 消费人数 */
    private BigDecimal periodAvgPerBuyer;
    /** 毛销售额（退款前，用于退款率分母） */
    private BigDecimal periodGrossSales;
    /** 区间内已完成售后的退款金额 */
    private BigDecimal periodRefundAmount;
    /** 退款率（%）= 退款金额 / 毛销售额 */
    private double periodRefundRate;
    /** 区间内完成的售后工单数 */
    private long periodRefundOrders;

    private List<TrendPointVO> trend;
    /** 24 小时分布（0-23，无数据补 0） */
    private List<HourStatVO> hourDist;
    /** 分类销售结构 */
    private List<CategoryStatVO> categoryStats;
    private List<ProductSalesVO> topProducts;
    private List<PayTypeStatVO> payTypes;
    private List<OrderTypeStatVO> orderTypes;
    private List<StatusCountVO> statusDist;
}
