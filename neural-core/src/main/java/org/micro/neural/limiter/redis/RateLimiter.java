package org.micro.neural.limiter.redis;

import com.google.common.math.LongMath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.StorePool;

import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流器
 *
 * @author lry
 */
@Slf4j
@Data
public class RateLimiter {

    /**
     * redis key
     */
    private String key;
    /**
     * 每秒存入的令牌数
     */
    private Double permitsPerSecond;
    /**
     * 最大存储maxBurstSeconds秒生成的令牌
     */
    private Integer maxBurstSeconds;

    public RateLimiter(String key, Double permitsPerSecond, Integer maxBurstSeconds) {
        this.key = key;
        this.permitsPerSecond = permitsPerSecond;
        this.maxBurstSeconds = maxBurstSeconds;
    }

    /**
     * 生成并存储默认令牌桶
     *
     * @return
     */
    private RedisPermits putDefaultPermits() {
        RedisPermits permits = new RedisPermits(permitsPerSecond, maxBurstSeconds);

        IStore store = StorePool.getInstance().getStore();

        String script = "";
        String[] keys = new String[]{key, String.valueOf(permits.expires()), SerializeUtils.serialize(permits)};
        String[] values = new String[]{};
        RedisPermits redisPermits = store.eval(RedisPermits.class, script, keys, values);

        this.lock();
        try {
            Object obj = RedisUtils.select().getValue(key);
            if (null == obj) {
                RedisPermits permits = new RedisPermits(permitsPerSecond, maxBurstSeconds);
                RedisUtils.select().addValue(key, permits, permits.expires(), TimeUnit.SECONDS);
                return permits;
            } else {
                return (RedisPermits) obj;
            }
        } finally {
            this.unlock();
        }
    }

    /**
     * 获取令牌桶
     *
     * @return
     */
    public RedisPermits getPermits() {
        IStore store = StorePool.getInstance().getStore();
        String obj = store.get(key);
        if (null == obj || obj.length() == 0) {
            return putDefaultPermits();
        }
        return SerializeUtils.deserialize(RedisPermits.class, obj);
    }

    /**
     * 更新令牌桶
     *
     * @param permits
     */
    public void setPermits(RedisPermits permits) {
        IStore store = StorePool.getInstance().getStore();
        RedisUtils.select().addValue(key, permits, permits.expires(), TimeUnit.SECONDS);
    }

    /**
     * 等待直到获取指定数量的令牌
     *
     * @param tokens
     * @return
     * @throws InterruptedException
     */
    public Long acquire(Long tokens) throws InterruptedException {
        long milliToWait = this.reserve(tokens);
        log.info("acquire for {}ms {}", milliToWait, Thread.currentThread().getName());
        Thread.sleep(milliToWait);
        return milliToWait;
    }

    /**
     * 获取1一个令牌
     *
     * @return
     * @throws InterruptedException
     */
    private long acquire() throws InterruptedException {
        return acquire(1L);
    }

    /**
     * @param tokens  要获取的令牌数
     * @param timeout 获取令牌等待的时间，负数被视为0
     * @param unit
     * @return
     * @throws InterruptedException
     */
    private Boolean tryAcquire(Long tokens, Long timeout, TimeUnit unit) throws InterruptedException {
        long timeoutMicros = Math.max(unit.toMillis(timeout), 0);
        checkTokens(tokens);
        Long milliToWait;

        IStore store = StorePool.getInstance().getStore();

        try {
            this.lock();
            if (!this.canAcquire(tokens, timeoutMicros)) {
                return false;
            } else {
                milliToWait = this.reserveAndGetWaitLength(tokens);
            }
        } finally {
            this.unlock();
        }
        Thread.sleep(milliToWait);
        return true;
    }

    /**
     * 获取一个令牌
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    private Boolean tryAcquire(Long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1L, timeout, unit);
    }

    private long redisNow() {
        IStore store = StorePool.getInstance().getStore();
        Long time = RedisUtils.select().currentTime();
        return null == time ? System.currentTimeMillis() : time;
    }

    /**
     * 获取令牌n个需要等待的时间
     *
     * @param tokens
     * @return
     */
    private long reserve(Long tokens) {
        this.checkTokens(tokens);

        IStore store = StorePool.getInstance().getStore();

        try {
            this.lock();
            return this.reserveAndGetWaitLength(tokens);
        } finally {
            this.unlock();
        }
    }

    /**
     * 校验token值
     *
     * @param tokens
     */
    private void checkTokens(Long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Requested tokens " + tokens + " must be positive");
        }
    }

    /**
     * 在等待的时间内是否可以获取到令牌
     *
     * @param tokens
     * @param timeoutMillis
     * @return
     */
    private Boolean canAcquire(Long tokens, Long timeoutMillis) {
        return queryEarliestAvailable(tokens) - timeoutMillis <= 0;
    }

    /**
     * 返回获取{tokens}个令牌最早可用的时间
     *
     * @param tokens
     * @return
     */
    private Long queryEarliestAvailable(Long tokens) {
        long n = redisNow();
        RedisPermits permit = this.getPermits();
        permit.reSync(n);
        // 可以消耗的令牌数
        long storedPermitsToSpend = Math.min(tokens, permit.getStoredPermits());
        // 需要等待的令牌数
        long freshPermits = tokens - storedPermitsToSpend;
        // 需要等待的时间
        long waitMillis = freshPermits * permit.getIntervalMillis();
        return LongMath.saturatedAdd(permit.getNextFreeTicketMillis() - n, waitMillis);
    }

    /**
     * 预定@{tokens}个令牌并返回所需要等待的时间
     *
     * @param tokens
     * @return
     */
    private Long reserveAndGetWaitLength(Long tokens) {
        long n = redisNow();
        RedisPermits permit = this.getPermits();
        permit.reSync(n);
        // 可以消耗的令牌数
        long storedPermitsToSpend = Math.min(tokens, permit.getStoredPermits());
        // 需要等待的令牌数
        long freshPermits = tokens - storedPermitsToSpend;
        // 需要等待的时间
        long waitMillis = freshPermits * permit.getIntervalMillis();
        permit.setNextFreeTicketMillis(LongMath.saturatedAdd(permit.getNextFreeTicketMillis(), waitMillis));
        permit.setStoredPermits(permit.getStoredPermits() - storedPermitsToSpend);
        this.setPermits(permit);
        return permit.getNextFreeTicketMillis() - n;
    }
}

