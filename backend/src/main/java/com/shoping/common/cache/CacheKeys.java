package com.shoping.common.cache;

import java.time.Duration;

/**
 * 缓存 key 规范与 TTL 定义（集中在一处，避免各服务各写一套前缀导致失效漏删）
 * <p>
 * 命名约定：{模块}:{类型}:{维度...}，全部由 {@link CacheProperties#getKeyPrefix()} 加前缀。
 * TTL 分级原则：越接近交易/库存的越短，纯配置类可以长。
 */
public final class CacheKeys {

    /** 商品列表分页 */
    public static final String PRODUCT_PAGE = "product:page:";
    /** 商品详情 */
    public static final String PRODUCT_DETAIL = "product:detail:";
    /** 分类列表 */
    public static final String CATEGORY_LIST = "category:list:";
    /** 门店信息（单门店，固定一条） */
    public static final String STORE_CURRENT = "store:current";
    /** 充值档位 */
    public static final String RECHARGE_RULES = "recharge:rules:";
    /** 商品评分汇总（平均分/星级分布/条数） */
    public static final String REVIEW_SUMMARY = "review:summary:";
    /** 销售看板 */
    public static final String STATS_DASHBOARD = "stats:dashboard:";

    /** 商品相关所有缓存（写商品后整片失效，列表与详情都可能变） */
    public static final String PRODUCT_ALL = "product:";
    /** 评分相关所有缓存 */
    public static final String REVIEW_ALL = "review:";

    // ===== TTL：短 TTL 兜底，即使某条写路径漏了失效也不会长期脏读 =====
    /** 列表带库存/评分，60 秒足够挡掉首页高频刷新 */
    public static final Duration TTL_PRODUCT_PAGE = Duration.ofSeconds(60);
    /** 详情含 SKU 库存，同样 60 秒 */
    public static final Duration TTL_PRODUCT_DETAIL = Duration.ofSeconds(60);
    /** 分类是纯配置，但带商品数，10 分钟 */
    public static final Duration TTL_CATEGORY = Duration.ofMinutes(10);
    /** 门店信息几乎不变，10 分钟 */
    public static final Duration TTL_STORE = Duration.ofMinutes(10);
    /** 充值档位运营偶尔改，5 分钟 */
    public static final Duration TTL_RECHARGE_RULES = Duration.ofMinutes(5);
    /** 评分汇总随评价变，5 分钟 */
    public static final Duration TTL_REVIEW_SUMMARY = Duration.ofMinutes(5);
    /** 看板是聚合查询最重，但要求近实时，30 秒 */
    public static final Duration TTL_STATS_DASHBOARD = Duration.ofSeconds(30);

    private CacheKeys() {
    }

    public static String productPage(Integer productType, Long categoryId, String keyword,
                                    long pageNum, long pageSize) {
        return PRODUCT_PAGE + nvl(productType) + ":" + nvl(categoryId) + ":"
                + normalize(keyword) + ":" + pageNum + ":" + pageSize;
    }

    public static String productDetail(Long productId) {
        return PRODUCT_DETAIL + productId;
    }

    public static String categoryList(Integer productType, boolean includeDisabled) {
        return CATEGORY_LIST + nvl(productType) + ":" + (includeDisabled ? "all" : "enabled");
    }

    public static String rechargeRules(boolean onlyEnabled) {
        return RECHARGE_RULES + (onlyEnabled ? "enabled" : "all");
    }

    public static String reviewSummary(Long productId) {
        return REVIEW_SUMMARY + productId;
    }

    public static String statsDashboard(Long storeId, int days) {
        return STATS_DASHBOARD + nvl(storeId) + ":" + days;
    }

    private static String nvl(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /** 关键词里的冒号会干扰 key 分段，统一折叠成下划线 */
    private static String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "-";
        }
        return keyword.trim().replace(':', '_').replace('*', '_');
    }
}
