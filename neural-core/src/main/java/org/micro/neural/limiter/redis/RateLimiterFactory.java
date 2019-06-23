package org.micro.neural.limiter.redis;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌桶限流器工厂
 *
 * @author lry
 */
@Slf4j
public class RateLimiterFactory {

    /**
     * 本地持有对象
     */
    private volatile Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    /**
     * @param key              redis key
     * @param permitsPerSecond 每秒产生的令牌数
     * @param maxBurstSeconds  最大存储多少秒的令牌
     * @return {@link RateLimiter}
     */
    public RateLimiter build(String key, Double permitsPerSecond, Integer maxBurstSeconds) {
        if (!rateLimiterMap.containsKey(key)) {
            synchronized (this) {
                if (!rateLimiterMap.containsKey(key)) {
                    rateLimiterMap.put(key, new RateLimiter(key, permitsPerSecond, maxBurstSeconds));
                }
            }
        }

        return rateLimiterMap.get(key);
    }

}
