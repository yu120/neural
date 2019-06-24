package org.micro.neural.limiter.core;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.RedisStore;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The Limiter pf Redis.
 * <p>
 * 1.Limit instantaneous concurrency
 * 2.Limit the maximum number of requests for a time window
 * 3.Token Bucket
 *
 * @author lry
 **/
@Slf4j
@Extension("redis")
public class RedisLimiter extends AbstractCallLimiter {

    private StorePool storePool = StorePool.getInstance();
    private static String CONCURRENCY_SCRIPT = getScript("/limiter_concurrency.lua");
    private static String RATE_SCRIPT = getScript("/limiter_rate.lua");
    private static String REQUEST_SCRIPT = getScript("/limiter_request.lua");

    @Override
    protected Acquire tryAcquireConcurrency() {
        IStore store = storePool.getStore();
        String[] keys = new String[]{limiterConfig.identity()};
        String[] values = new String[]{String.valueOf(limiterConfig.getConcurrency()), "0"};

        try {
            Integer result = store.eval(Integer.class, CONCURRENCY_SCRIPT,
                    limiterConfig.getConcurrencyTimeout(), keys, values);
            if (result == null) {
                return Acquire.EXCEPTION;
            } else if (result == 0) {
                return Acquire.FAILURE;
            } else {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void releaseAcquireConcurrency() {
        IStore store = storePool.getStore();
        String[] keys = new String[]{limiterConfig.identity()};
        String[] values = new String[]{String.valueOf(limiterConfig.getConcurrency()), "1"};

        try {
            store.eval(Integer.class, CONCURRENCY_SCRIPT,
                    limiterConfig.getConcurrencyTimeout(), keys, values);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Acquire tryAcquireRequest() {
        IStore store = storePool.getStore();
        String[] keys = new String[]{limiterConfig.identity()};
        String[] values = new String[]{String.valueOf(limiterConfig.getRequestMaxPermits())};

        try {
            Integer result = store.eval(Integer.class, REQUEST_SCRIPT,
                    limiterConfig.getRequestTimeout(), keys, values);
            if (result == null) {
                return Acquire.EXCEPTION;
            } else if (result == 0) {
                return Acquire.FAILURE;
            } else {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireRateLimiter() {
        return null;
    }

    private static String getScript(String name) {
        try {
            return CharStreams.toString(new InputStreamReader(
                    RedisStore.class.getResourceAsStream(name), Charsets.UTF_8));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
