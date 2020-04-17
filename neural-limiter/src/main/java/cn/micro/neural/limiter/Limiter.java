package cn.micro.neural.limiter;

import cn.micro.neural.limiter.core.ILimiter;
import cn.neural.common.extension.Extension;
import cn.neural.common.extension.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Limiter.
 *
 * @author lry
 **/
@Slf4j
@Extension(EventType.IDENTITY)
public class Limiter {

    private final ConcurrentMap<String, ILimiter> limiters = new ConcurrentHashMap<>();

    /**
     * The add limiter
     *
     * @param limiterConfig {@link LimiterConfig}
     */
    public void addLimiter(LimiterConfig limiterConfig) {
        ILimiter limiter = ExtensionLoader.getLoader(ILimiter.class).getExtension(limiterConfig.getMode().getValue());
        limiters.put(limiterConfig.identity(), limiter);
    }

    /**
     * The notify of changed config
     *
     * @param limiterConfig {@link LimiterConfig}
     */
    public void notify(LimiterConfig limiterConfig) {
        try {
            ILimiter limiter = limiters.get(limiterConfig.identity());
            if (null == limiter) {
                log.warn("The limiter config is notify is exception, not found limiter: {}", limiterConfig);
                return;
            }
            if (!limiter.refresh(limiterConfig)) {
                log.warn("The limiter refresh failure: {}", limiterConfig);
            }
        } catch (Exception e) {
            //EventCollect.onEvent(LimiterGlobalConfig.EventType.NOTIFY_EXCEPTION);
            log.error(EventType.NOTIFY_EXCEPTION.getMessage(), e);
        }
    }

    /**
     * The process of original call
     *
     * @param identity     {@link LimiterConfig#identity()}
     * @param originalCall {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    public Object originalCall(String identity, OriginalCall originalCall) throws Throwable {
        return originalCall(new LimiterContext(), identity, originalCall);
    }

    /**
     * The process of original call
     *
     * @param limiterContext {@link LimiterContext}
     * @param identity       {@link LimiterConfig#identity()}
     * @param originalCall   {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    public Object originalCall(final LimiterContext limiterContext, String identity, OriginalCall originalCall) throws Throwable {
        try {
            LimiterContext.set(limiterContext);
            // The check limiter object
            if (null == identity || !limiters.containsKey(identity)) {
                return originalCall.call();
            }

            return limiters.get(identity).wrapperCall(limiterContext, originalCall);
        } finally {
            LimiterContext.remove();
        }
    }

    /**
     * The collect of get and reset statistics data
     *
     * @return statistics data
     */
    public Map<String, Map<String, Long>> collect() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        try {
            limiters.forEach((identity, limiter) -> {
                Map<String, Long> tempDataMap = limiter.getStatistics().getAndReset();
                if (null == tempDataMap || tempDataMap.isEmpty()) {
                    return;
                }

                dataMap.put(identity, tempDataMap);
            });
        } catch (Exception e) {
            //EventCollect.onEvent(LimiterGlobalConfig.EventType.COLLECT_EXCEPTION);
            log.error(EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

    /**
     * The get statistics data
     *
     * @return statistics data
     */
    public Map<String, Map<String, Long>> statistics() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        try {
            limiters.forEach((identity, limiter) -> {
                Map<String, Long> tempDataMap = limiter.getStatistics().getStatisticsData();
                if (null == tempDataMap || tempDataMap.isEmpty()) {
                    return;
                }

                dataMap.put(identity, tempDataMap);
            });
        } catch (Exception e) {
            //EventCollect.onEvent(LimiterGlobalConfig.EventType.COLLECT_EXCEPTION);
            log.error(EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

}
