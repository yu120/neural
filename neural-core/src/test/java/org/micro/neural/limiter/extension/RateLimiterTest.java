package org.micro.neural.limiter.extension;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RateLimiterTest {

    public static void main(String[] args) throws Exception {
        AdjustableRateLimiter adjustableRateLimiter = AdjustableRateLimiter.create(5, 2);
        for (int i = 0; i < 10; i++) {
            System.out.println(adjustableRateLimiter.acquire());
        }
        System.out.println(adjustableRateLimiter.toString());
        System.out.println(adjustableRateLimiter.getRate());
    }

    /**
     * 平均200ms获取1个
     *
     * @throws Exception
     */
    @Test
    public void testAvgRate() throws Exception {
        AdjustableRateLimiter adjustableRateLimiter = AdjustableRateLimiter.create(5);
        assert 0 == adjustableRateLimiter.acquire();
        assert 2 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire() * 10);
    }

    /**
     * 测试突发
     *
     * @throws Exception
     */
    @Test
    public void burstTest() throws Exception {
        AdjustableRateLimiter adjustableRateLimiter = AdjustableRateLimiter.create(5);
        assert 0 == adjustableRateLimiter.acquire(5);
        assert 1 == Math.round(adjustableRateLimiter.acquire(1));
        assert 2 == Math.round(adjustableRateLimiter.acquire(1) * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire(1) * 10);
        assert 2 == Math.round(adjustableRateLimiter.acquire(1) * 10);

        AdjustableRateLimiter rateLimiter1 = AdjustableRateLimiter.create(5);
        assert 0 == Math.round(rateLimiter1.acquire(10));
        assert 2 == Math.round(rateLimiter1.acquire(1));
        assert 2 == Math.round(rateLimiter1.acquire(1) * 10);
    }

    /**
     * 测试积攒量
     *
     * @throws Exception
     */
    @Test
    public void accumulateTest() throws Exception {
        AdjustableRateLimiter adjustableRateLimiter = AdjustableRateLimiter.create(2);
        assert 0 == adjustableRateLimiter.acquire();
        Thread.sleep(4000);
        assert 0 == adjustableRateLimiter.acquire();
        assert 0 == adjustableRateLimiter.acquire();
        assert 0 == adjustableRateLimiter.acquire();
        assert 5 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 5 == Math.round(adjustableRateLimiter.acquire() * 10);
        Thread.sleep(2000);
        assert 0 == adjustableRateLimiter.acquire();
        assert 0 == adjustableRateLimiter.acquire();
        assert 0 == adjustableRateLimiter.acquire();
        assert 5 == Math.round(adjustableRateLimiter.acquire() * 10);
        assert 5 == Math.round(adjustableRateLimiter.acquire() * 10);
    }

    @Test
    public void smoothWarmingTest() throws Exception {
        /**
         * 每秒新增令牌数为5, 1000ms内速率缓慢降至平均速率
         * <p>
         * 平均200ms新增一个
         */
        AdjustableRateLimiter limiter = AdjustableRateLimiter.create(5, 1000, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 10; i++) {
            System.out.println(limiter.acquire());

        }
        Thread.sleep(2000);
        for (int i = 0; i < 10; i++) {
            System.out.println(limiter.acquire());
        }
    }

    @Test
    public void testTry() throws Exception {
        AdjustableRateLimiter adjustableRateLimiter = AdjustableRateLimiter.create(1);
        for (int i = 0; i < 10; i++) {
            System.out.println(adjustableRateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS));
        }
    }

}
