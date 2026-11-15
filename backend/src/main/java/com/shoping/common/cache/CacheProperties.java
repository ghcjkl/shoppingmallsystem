package com.shoping.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存配置项（见 application.yml 的 app.cache）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    /** 缓存总开关：false 时所有读请求直接回源数据库 */
    private boolean enabled = true;

    /** key 统一前缀，便于运维按前缀清理与隔离多环境 */
    private String keyPrefix = "shoping:";

    /** Redis 异常后熔断时长（秒），期间跳过缓存直接回源，避免每次请求都等超时 */
    private int failRetrySeconds = 30;
}
