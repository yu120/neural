package org.micro.neural.limiter.core;

import org.micro.neural.extension.Extension;

/**
 * The Limiter pf Redis.
 *
 * @author lry
 **/
@Extension("redis")
public class RedisLimiter extends AbstractCallLimiter {

    @Override
    protected Acquire tryAcquireConcurrency() {
        return null;
    }

    @Override
    protected void releaseAcquireConcurrency() {

    }

    @Override
    protected Acquire tryAcquireRateLimiter() {
        return null;
    }

}
