package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.storage.FactoryStorage;
import cn.neural.common.extension.Extension;
import cn.neural.common.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Cluster Limiter by Redis.
 * <p>
 * 1.Limit instantaneous concurrent
 * 2.Limit the maximum number of counter for a time window
 * 3.Token Bucket
 *
 * @author lry
 **/
@Slf4j
@Extension("cluster")
public class ClusterLimiter extends AbstractCallLimiter {

    private static String CONCURRENT_SCRIPT = StreamUtils.loadScript("concurrent_limiter.lua");
    private static String COUNTER_SCRIPT = StreamUtils.loadScript("counter_limiter.lua");
    private static String RATE_SCRIPT = StreamUtils.loadScript("rate_limiter.lua");

    @Override
    protected boolean tryRefresh(LimiterConfig config) {
        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrent() {
        LimiterConfig.ConcurrentLimiterConfig concurrentConfig = config.getConcurrent();
        List<String> keys = Collections.singletonList(config.identity());
        List<Object> values = Arrays.asList(concurrentConfig.getPermitUnit(),
                concurrentConfig.getMaxPermit(), concurrentConfig.getTimeout());

        try {
            Number[] result = FactoryStorage.INSTANCE.getStorage().eval(CONCURRENT_SCRIPT, keys, values);
            if (result == null || result.length != 2) {
                return Acquire.EXCEPTION;
            }

            return Acquire.valueOf(result[0].intValue());
        } catch (Exception e) {
            log.error("Try acquire cluster concurrent exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void releaseConcurrent() {
        LimiterConfig.ConcurrentLimiterConfig concurrentConfig = config.getConcurrent();
        List<String> keys = Collections.singletonList(config.identity());
        List<Object> values = Arrays.asList(-concurrentConfig.getPermitUnit(),
                concurrentConfig.getMaxPermit(), concurrentConfig.getTimeout());

        try {
            FactoryStorage.INSTANCE.getStorage().eval(CONCURRENT_SCRIPT, keys, values);
        } catch (Exception e) {
            log.error("Try release cluster concurrent exception", e);
        }
    }

    @Override
    protected Acquire tryAcquireRate() {
        LimiterConfig.RateLimiterConfig rateConfig = config.getRate();
        List<String> keys = Collections.singletonList(config.identity());
        List<Object> values = Arrays.asList(rateConfig.getRateUnit(), rateConfig.getMaxRate());

        try {
            Number[] result = FactoryStorage.INSTANCE.getStorage().eval(RATE_SCRIPT, keys, values);
            if (result == null || result.length != 2) {
                return Acquire.EXCEPTION;
            }

            return Acquire.valueOf(result[0].intValue());
        } catch (Exception e) {
            log.error("Try acquire cluster rate exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireCounter() {
        LimiterConfig.CounterLimiterConfig counterConfig = config.getCounter();
        List<String> keys = Collections.singletonList(config.identity());
        List<Object> values = Arrays.asList(counterConfig.getCountUnit(),
                counterConfig.getMaxCount(), counterConfig.getTimeout());

        try {
            Number[] result = FactoryStorage.INSTANCE.getStorage().eval(COUNTER_SCRIPT, keys, values);
            if (result == null || result.length != 2) {
                return Acquire.EXCEPTION;
            }

            return Acquire.valueOf(result[0].intValue());
        } catch (Exception e) {
            log.error("Try acquire cluster counter exception", e);
            return Acquire.EXCEPTION;
        }
    }

}
