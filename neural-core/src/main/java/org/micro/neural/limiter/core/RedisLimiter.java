package org.micro.neural.limiter.core;

import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;

/**
 * The Limiter pf Redis.
 *
 * @author lry
 **/
@Extension("redis")
public class RedisLimiter extends AbstractCallLimiter {

    private StorePool storePool = StorePool.getInstance();

    @Override
    protected Acquire tryAcquireConcurrency() {
        IStore store = storePool.getStore();
        String identity = limiterConfig.identity();
        Integer result = store.increment(identity,
                limiterConfig.getConcurrency(), limiterConfig.getConcurrencyTimeout());
        if (result == null) {
            return Acquire.EXCEPTION;
        } else if (result == 0) {
            return Acquire.FAILURE;
        } else {
            return Acquire.SUCCESS;
        }
    }

    @Override
    protected void releaseAcquireConcurrency() {

    }

    @Override
    protected Acquire tryAcquireRateLimiter() {
        return null;
    }

}
