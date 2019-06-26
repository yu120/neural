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
import java.util.ArrayList;
import java.util.List;

/**
 * The Limiter pf Redis.
 * <p>
 * 1.Limit instantaneous concurrent
 * 2.Limit the maximum number of requests for a time window
 * 3.Token Bucket
 *
 * @author lry
 **/
@Slf4j
@Extension("redis")
public class ClusterLimiter extends AbstractCallLimiter {

    private StorePool storePool = StorePool.getInstance();
    private static String CONCURRENT_SCRIPT = getScript("/limiter_concurrent.lua");
    private static String RATE_SCRIPT = getScript("/limiter_rate.lua");
    private static String REQUEST_SCRIPT = getScript("/limiter_request.lua");

    @Override
    protected Acquire incrementConcurrent() {
        IStore store = storePool.getStore();
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getConcurrentPermit());
        keys.add(limiterConfig.getMaxPermitConcurrent());

        try {
            Integer result = store.eval(Integer.class, CONCURRENT_SCRIPT, limiterConfig.getConcurrentTimeout(), keys);
            return result == null ? Acquire.EXCEPTION : (result == 0 ? Acquire.FAILURE : Acquire.SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void decrementConcurrent() {
        IStore store = storePool.getStore();
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(-limiterConfig.getConcurrentPermit());
        keys.add(limiterConfig.getMaxPermitConcurrent());

        try {
            store.eval(Integer.class, CONCURRENT_SCRIPT, limiterConfig.getConcurrentTimeout(), keys);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Acquire tryAcquireRate() {
        IStore store = storePool.getStore();
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getRatePermit());
        keys.add(System.currentTimeMillis());
        keys.add("app");

        try {
            Integer result = store.eval(Integer.class, RATE_SCRIPT, limiterConfig.getRateTimeout(), keys);
            return result == null ? Acquire.EXCEPTION : (result == 0 ? Acquire.FAILURE : Acquire.SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireRequest() {
        IStore store = storePool.getStore();
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getMaxPermitRequest());
        //TODO
        keys.add(limiterConfig.getRequestInterval().toMillis());

        try {
            Integer result = store.eval(Integer.class, REQUEST_SCRIPT, limiterConfig.getRequestTimeout(), keys);
            return result == null ? Acquire.EXCEPTION : (result == 0 ? Acquire.FAILURE : Acquire.SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
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
