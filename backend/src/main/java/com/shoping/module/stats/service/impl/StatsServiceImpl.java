package com.shoping.module.stats.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.shoping.common.cache.CacheKeys;
import com.shoping.common.cache.CacheService;
import com.shoping.module.stats.mapper.StatsMapper;
import com.shoping.module.stats.service.StatsService;
import com.shoping.module.stats.vo.CategoryStatVO;
import com.shoping.module.stats.vo.DashboardVO;
import com.shoping.module.stats.vo.HourStatVO;
import com.shoping.module.stats.vo.PeriodStatVO;
import com.shoping.module.stats.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;
    private final CacheService cacheService;

    @Override
    public DashboardVO dashboard(Long storeId, int days) {
        int span = days <= 0 ? 7 : Math.min(days, 90);
        Long store = storeId == null ? 1L : storeId;
        // 看板是一整套聚合查询（10 条以上 SQL），30 秒缓存足够挡住管理端反复切换天数
        return cacheService.remember(CacheKeys.statsDashboard(store, span), CacheKeys.TTL_STATS_DASHBOARD,
                new TypeReference<DashboardVO>() {
                }, () -> loadDashboard(store, span));
    }

    private DashboardVO loadDashboard(Long store, int span) {
        LocalDate today = LocalDate.now();
        LocalDateTime periodStart = today.minusDays(span - 1L).atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        DashboardVO vo = new DashboardVO();
        vo.setDays(span);

        // 今日 / 昨日
        PeriodStatVO todayStat = safe(statsMapper.periodStat(store, todayStart, tomorrow));
        PeriodStatVO yesterdayStat = safe(statsMapper.periodStat(store, yesterdayStart, todayStart));
        vo.setTodaySales(todayStat.getSales());
        vo.setTodayOrders(nvl(todayStat.getOrders()));
        vo.setYesterdaySales(yesterdayStat.getSales());
        vo.setYesterdayOrders(nvl(yesterdayStat.getOrders()));
        vo.setTodayCreated(statsMapper.createdOrders(store, todayStart, tomorrow));
        vo.setTodayAvgTicket(avg(todayStat.getSales(), nvl(todayStat.getOrders())));
        vo.setSalesRate(rate(todayStat.getSales(), yesterdayStat.getSales()));
        vo.setOrdersRate(rate(BigDecimal.valueOf(nvl(todayStat.getOrders())),
                BigDecimal.valueOf(nvl(yesterdayStat.getOrders()))));

        // 区间趋势（补齐没有订单的日期，保证折线连续）
        List<TrendPointVO> raw = statsMapper.dailyTrend(store, periodStart, tomorrow);
        Map<LocalDate, TrendPointVO> byDate = new HashMap<>();
        for (TrendPointVO point : raw) {
            if (point.getStatDate() != null) {
                byDate.put(point.getStatDate(), point);
            }
        }
        List<TrendPointVO> trend = new ArrayList<>();
        BigDecimal periodSales = BigDecimal.ZERO;
        long periodOrders = 0;
        for (int i = 0; i < span; i++) {
            LocalDate date = periodStart.toLocalDate().plusDays(i);
            TrendPointVO point = byDate.get(date);
            if (point == null) {
                point = new TrendPointVO();
                point.setStatDate(date);
                point.setOrders(0L);
                point.setSales(BigDecimal.ZERO);
            }
            periodSales = periodSales.add(point.getSales() == null ? BigDecimal.ZERO : point.getSales());
            periodOrders += nvl(point.getOrders());
            trend.add(point);
        }
        vo.setTrend(trend);
        vo.setPeriodSales(periodSales);
        vo.setPeriodOrders(periodOrders);

        vo.setTopProducts(statsMapper.topProducts(store, periodStart, tomorrow));
        vo.setPayTypes(statsMapper.payTypes(store, periodStart, tomorrow));
        vo.setOrderTypes(statsMapper.orderTypes(store, periodStart, tomorrow));
        vo.setStatusDist(statsMapper.statusDist(store, periodStart, tomorrow));

        // ===== 经营分析：时段 / 会员 / 退款 / 分类 =====
        vo.setHourDist(fillHours(statsMapper.hourDist(store, periodStart, tomorrow)));

        long buyers = statsMapper.periodBuyers(store, periodStart, tomorrow);
        long newBuyers = statsMapper.periodNewBuyers(store, periodStart, tomorrow);
        vo.setPeriodBuyers(buyers);
        vo.setPeriodNewBuyers(Math.min(newBuyers, buyers));
        vo.setPeriodAvgPerBuyer(buyers > 0
                ? periodSales.divide(BigDecimal.valueOf(buyers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        BigDecimal gross = nvlMoney(statsMapper.grossSales(store, periodStart, tomorrow));
        BigDecimal refund = nvlMoney(statsMapper.periodRefundAmount(store, periodStart, tomorrow));
        vo.setPeriodGrossSales(gross);
        vo.setPeriodRefundAmount(refund);
        vo.setPeriodRefundOrders(statsMapper.periodRefundOrders(store, periodStart, tomorrow));
        vo.setPeriodRefundRate(gross.compareTo(BigDecimal.ZERO) > 0
                ? refund.divide(gross, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0D);

        vo.setCategoryStats(normalizeCategories(statsMapper.categoryStats(store, periodStart, tomorrow)));
        return vo;
    }

    /** 补齐 0-23 时，保证柱状图横轴完整 */
    private List<HourStatVO> fillHours(List<HourStatVO> raw) {
        Map<Integer, HourStatVO> byHour = new HashMap<>();
        if (raw != null) {
            for (HourStatVO point : raw) {
                if (point.getStatHour() != null) {
                    byHour.put(point.getStatHour(), point);
                }
            }
        }
        List<HourStatVO> result = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            HourStatVO point = byHour.get(hour);
            if (point == null) {
                point = new HourStatVO();
                point.setStatHour(hour);
                point.setOrders(0L);
                point.setSales(BigDecimal.ZERO);
            }
            if (point.getSales() == null) {
                point.setSales(BigDecimal.ZERO);
            }
            if (point.getOrders() == null) {
                point.setOrders(0L);
            }
            result.add(point);
        }
        return result;
    }

    /** 商品已删除时拿不到分类名，统一归入「未分类」并合并 */
    private List<CategoryStatVO> normalizeCategories(List<CategoryStatVO> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, CategoryStatVO> merged = new LinkedHashMap<>();
        for (CategoryStatVO item : raw) {
            String name = (item.getName() == null || item.getName().isBlank()) ? "未分类" : item.getName();
            CategoryStatVO target = merged.get(name);
            if (target == null) {
                item.setName(name);
                item.setQty(nvl(item.getQty()));
                item.setAmount(nvlMoney(item.getAmount()));
                merged.put(name, item);
            } else {
                target.setQty(nvl(target.getQty()) + nvl(item.getQty()));
                target.setAmount(nvlMoney(target.getAmount()).add(nvlMoney(item.getAmount())));
            }
        }
        List<CategoryStatVO> list = new ArrayList<>(merged.values());
        list.sort((a, b) -> nvlMoney(b.getAmount()).compareTo(nvlMoney(a.getAmount())));
        return list;
    }

    private BigDecimal nvlMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private PeriodStatVO safe(PeriodStatVO stat) {
        if (stat == null) {
            stat = new PeriodStatVO();
            stat.setSales(BigDecimal.ZERO);
            stat.setOrders(0L);
        }
        if (stat.getSales() == null) {
            stat.setSales(BigDecimal.ZERO);
        }
        return stat;
    }

    private BigDecimal avg(BigDecimal sales, long orders) {
        if (orders <= 0) {
            return BigDecimal.ZERO;
        }
        return sales.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }

    /** 环比：昨日为 0 时，今天有数据记 100%，否则 0% */
    private double rate(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current == null ? BigDecimal.ZERO : current;
        BigDecimal pre = previous == null ? BigDecimal.ZERO : previous;
        if (pre.compareTo(BigDecimal.ZERO) == 0) {
            return cur.compareTo(BigDecimal.ZERO) > 0 ? 100D : 0D;
        }
        return cur.subtract(pre)
                .divide(pre, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
