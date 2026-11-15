package com.shoping.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 缓存门面：只暴露业务需要的三个动作，实现层负责序列化、TTL 抖动与降级。
 * <p>
 * 约定：任何缓存异常都不向业务抛出，{@link #remember} 会自动回源，保证 Redis 挂了接口仍可用。
 */
public interface CacheService {

    /**
     * 读缓存，未命中则执行 loader 并回填。
     * <p>
     * 用 TypeReference 而不是 Class，是为了让分页结果这类泛型对象能正确反序列化。
     */
    <T> T remember(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader);

    /** 精确删除若干 key（写操作后调用） */
    void evict(String... keys);

    /** 按前缀批量删除（如商品改动后清掉该商品全部列表页缓存），内部用 SCAN 不用 KEYS */
    void evictByPrefix(String prefix);

    /** 当前是否可用（开关打开且未处于熔断期） */
    boolean available();
}
