package com.shoping.config;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 登录限流（内存版）：按「账号 + IP」维度记录失败时间戳，防止暴力破解。
 * <p>
 * 规则（可配置）：
 * <ul>
 *   <li>连续失败达到 {@code maxFailures} 次后，锁定 {@code lockSeconds} 秒；</li>
 *   <li>窗口 {@code windowSeconds} 秒内失败次数达到阈值即触发锁定。</li>
 * </ul>
 * 单机/演示环境足够；多实例需替换为 Redis。登录成功会清空该维度计数。
 */
@Component
public class LoginRateLimiter {

    /** 窗口时长（秒） */
    private final long windowSeconds = 300L;

    /** 窗口内允许的最大失败次数 */
    private final int maxFailures = 5;

    /** 触发锁定后的锁定时长（秒） */
    private final long lockSeconds = 600L;

    /** key -> 失败时间戳队列 */
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    /** key -> 锁定截止时间戳（ms） */
    private final Map<String, Long> lockedUntil = new ConcurrentHashMap<>();

    private String key(String account, String ip) {
        return account + "@" + ip;
    }

    /**
     * 是否处于锁定状态
     */
    public boolean isLocked(String account, String ip) {
        Long until = lockedUntil.get(key(account, ip));
        if (until == null) {
            return false;
        }
        if (until > System.currentTimeMillis()) {
            return true;
        }
        lockedUntil.remove(key(account, ip));
        return false;
    }

    /**
     * 剩余锁定时长（秒），未锁定返回 0
     */
    public long remainLockSeconds(String account, String ip) {
        Long until = lockedUntil.get(key(account, ip));
        if (until == null) {
            return 0;
        }
        long remain = (until - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 0);
    }

    /**
     * 记录一次失败，返回是否因此触发锁定
     */
    public boolean recordFailure(String account, String ip) {
        String k = key(account, ip);
        long now = System.currentTimeMillis();
        Deque<Long> deque = failures.computeIfAbsent(k, x -> new ConcurrentLinkedDeque<>());
        deque.addLast(now);
        // 清理窗口外的旧记录
        long cutoff = now - windowSeconds * 1000L;
        while (!deque.isEmpty() && deque.peekFirst() != null && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
        if (deque.size() >= maxFailures) {
            lockedUntil.put(k, now + lockSeconds * 1000L);
            deque.clear();
            return true;
        }
        return false;
    }

    /**
     * 登录成功，清空该维度计数
     */
    public void clear(String account, String ip) {
        failures.remove(key(account, ip));
        lockedUntil.remove(key(account, ip));
    }
}
