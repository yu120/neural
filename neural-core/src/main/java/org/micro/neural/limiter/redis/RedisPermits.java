package org.micro.neural.limiter.redis;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * redis令牌桶
 *
 * @author lry
 */
@Data
@ToString
public class RedisPermits implements Serializable {

    private static final long serialVersionUID = -193070121323334106L;

    /**
     * maxPermits 最大存储令牌数
     */
    private Long maxPermits;
    /**
     * storedPermits 当前存储令牌数
     */
    private Long storedPermits;
    /**
     * intervalMillis 添加令牌时间间隔
     */
    private Long intervalMillis;
    /**
     * nextFreeTicketMillis 下次请求可以获取令牌的起始时间，默认当前系统时间
     */
    private Long nextFreeTicketMillis;

    /**
     * @param permitsPerSecond 每秒放入的令牌数
     * @param maxBurstSeconds  maxPermits由此字段计算，最大存储maxBurstSeconds秒生成的令牌
     */
    public RedisPermits(Double permitsPerSecond, Integer maxBurstSeconds) {
        if (null == maxBurstSeconds) {
            maxBurstSeconds = 60;
        }
        this.maxPermits = (long) (permitsPerSecond * maxBurstSeconds);
        this.storedPermits = permitsPerSecond.longValue();
        this.intervalMillis = (long) (TimeUnit.SECONDS.toMillis(1) / permitsPerSecond);
        this.nextFreeTicketMillis = System.currentTimeMillis();
    }

    /**
     * redis的过期时长
     *
     * @return
     */
    public long expires() {
        long now = System.currentTimeMillis();
        return 2 * TimeUnit.MINUTES.toSeconds(1) +
                TimeUnit.MILLISECONDS.toSeconds(Math.max(nextFreeTicketMillis, now) - now);
    }

    /**
     * 异步更新当前持有的令牌数
     * 若当前时间晚于nextFreeTicketMicros，则计算该段时间内可以生成多少令牌，将生成的令牌加入令牌桶中并更新数据
     *
     * @param now
     * @return
     */
    public boolean reSync(long now) {
        if (now > nextFreeTicketMillis) {
            storedPermits = Math.min(maxPermits, storedPermits + (now - nextFreeTicketMillis) / intervalMillis);
            nextFreeTicketMillis = now;
            return true;
        }
        return false;
    }

}

