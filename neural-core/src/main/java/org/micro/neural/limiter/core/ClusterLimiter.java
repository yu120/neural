package org.micro.neural.limiter.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.utils.StreamUtils;
import org.micro.neural.config.store.RedisStore;
import org.micro.neural.extension.Extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Cluster Limiter by Redis.
 * <p>
 * 1.Limit instantaneous concurrent
 * 2.Limit the maximum number of requests for a time window
 * 3.Token Bucket
 *
 * @author lry
 **/
@Slf4j
@Extension("cluster")
public class ClusterLimiter extends AbstractCallLimiter {

    private static String CONCURRENT_SCRIPT = StreamUtils.loadScript("/script/limiter_concurrent.lua");
    private static String RATE_SCRIPT = StreamUtils.loadScript("/script/limiter_rate.lua");
    private static String REQUEST_SCRIPT = StreamUtils.loadScript("/script/limiter_request.lua");

    @Override
    protected Acquire incrementConcurrent() {
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getConcurrentPermit());
        keys.add(limiterConfig.getMaxPermitConcurrent());

        try {
            EvalResult evalResult = eval(CONCURRENT_SCRIPT, limiterConfig.getConcurrentTimeout(), keys);
            System.out.println("The increment concurrent:" + evalResult);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void decrementConcurrent() {
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(-limiterConfig.getConcurrentPermit());
        keys.add(limiterConfig.getMaxPermitConcurrent());

        try {
            EvalResult evalResult = eval(CONCURRENT_SCRIPT, limiterConfig.getConcurrentTimeout(), keys);
            System.out.println("The decrement concurrent:" + evalResult);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Acquire tryAcquireRate() {
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getRatePermit());
        keys.add(System.currentTimeMillis());
        keys.add("app");

        try {
            EvalResult evalResult = eval(RATE_SCRIPT, limiterConfig.getRateTimeout(), keys);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireRequest() {
        List<Object> keys = new ArrayList<>();
        keys.add(limiterConfig.identity());
        keys.add(limiterConfig.getRequestPermit());
        keys.add(limiterConfig.getMaxPermitRequest());
        keys.add(limiterConfig.getRequestInterval().toMillis());

        try {
            EvalResult evalResult = eval(REQUEST_SCRIPT, limiterConfig.getRequestTimeout(), keys);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    private EvalResult eval(String script, Long timeout, List<Object> keys) {
        List<Object> result = RedisStore.INSTANCE.eval(script, timeout, keys);
        if (result == null || result.size() != 2) {
            return new EvalResult(Acquire.EXCEPTION, 0L);
        }

        Acquire acquire = Acquire.valueOf((int) result.get(0));
        return new EvalResult(acquire, (long) result.get(1));
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class EvalResult implements Serializable {

        private static final long serialVersionUID = 965512125433109743L;

        private Acquire code;
        private Long num;

    }

}
