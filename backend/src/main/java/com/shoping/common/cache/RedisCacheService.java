package com.shoping.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Redis 缓存实现：String + JSON 序列化。
 * <p>
 * 三个工程要点：
 * 1. 降级：Redis 任何异常都回源，业务无感；连续失败后熔断一段时间，避免每次请求都等 800ms 超时。
 * 2. 防雪崩：TTL 加 ±10% 抖动，避免同一批 key 同时过期把请求全打到数据库。
 * 3. 安全失效：按前缀删除用 SCAN 游标分批，绝不用 KEYS 阻塞 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    /** 熔断到期时间戳：当前时间小于该值时直接跳过缓存 */
    private final AtomicLong retryAt = new AtomicLong(0L);

    @Override
    public <T> T remember(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        if (!available()) {
            return loader.get();
        }
        String fullKey = fullKey(key);
        try {
            String cached = redis.opsForValue().get(fullKey);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception e) {
            // 读失败（连接断了/脏数据）：降级回源，不能因为缓存把接口打挂
            trip("读取", e);
            return loader.get();
        }

        T value = loader.get();
        if (value != null) {
            try {
                redis.opsForValue().set(fullKey, objectMapper.writeValueAsString(value), jitter(ttl));
            } catch (Exception e) {
                trip("写入", e);
            }
        }
        return value;
    }

    @Override
    public void evict(String... keys) {
        if (!properties.isEnabled() || keys == null || keys.length == 0) {
            return;
        }
        try {
            List<String> fullKeys = new ArrayList<>(keys.length);
            for (String key : keys) {
                if (key != null && !key.isBlank()) {
                    fullKeys.add(fullKey(key));
                }
            }
            if (!fullKeys.isEmpty()) {
                redis.delete(fullKeys);
            }
        } catch (Exception e) {
            trip("删除", e);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        if (!properties.isEnabled() || prefix == null || prefix.isBlank()) {
            return;
        }
        String pattern = fullKey(prefix) + "*";
        try {
            long removed = 0;
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            List<String> batch = new ArrayList<>(200);
            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 200) {
                        removed += deleteBatch(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    removed += deleteBatch(batch);
                }
            }
            if (removed > 0) {
                log.info("缓存前缀清理完成：{}，共删除 {} 个 key", prefix, removed);
            }
        } catch (Exception e) {
            trip("前缀删除", e);
        }
    }

    @Override
    public boolean available() {
        return properties.isEnabled() && System.currentTimeMillis() >= retryAt.get();
    }

    private long deleteBatch(List<String> keys) {
        Long deleted = redis.delete(keys);
        return deleted == null ? 0L : deleted;
    }

    /** TTL 加 ±10% 抖动，最小 1 秒 */
    private Duration jitter(Duration ttl) {
        long base = ttl.toMillis();
        long delta = Math.max(1L, Math.round(base * 0.1));
        long millis = base - delta + ThreadLocalRandom.current().nextLong(delta * 2 + 1);
        return Duration.ofMillis(Math.max(1000L, millis));
    }

    private String fullKey(String key) {
        String prefix = properties.getKeyPrefix() == null ? "" : properties.getKeyPrefix();
        return prefix + key;
    }

    private void trip(String action, Exception e) {
        int seconds = Math.max(1, properties.getFailRetrySeconds());
        retryAt.set(System.currentTimeMillis() + seconds * 1000L);
        log.warn("Redis {}失败，{} 秒内跳过缓存直接回源：{}", action, seconds, e.getMessage());
    }
}
