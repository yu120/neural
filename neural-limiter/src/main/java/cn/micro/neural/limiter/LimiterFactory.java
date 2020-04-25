package cn.micro.neural.limiter;

import cn.micro.neural.limiter.core.ILimiter;
import cn.neural.common.extension.Extension;
import cn.neural.common.extension.ExtensionLoader;
import lombok.Getter;
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
@Getter
@Extension(LimiterFactory.IDENTITY)
public class LimiterFactory {

    public static final String IDENTITY = "limiter";

    /**
     * Map<key=ILimiter#identity(), ILimiter>
     */
    private final ConcurrentMap<String, ILimiter> limiters = new ConcurrentHashMap<>();
    /**
     * Map<key=group, subKey=tag, value=LimiterConfig>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, LimiterConfig>> rules = new ConcurrentHashMap<>();

    /**
     * The add limiter
     *
     * @param group {@link LimiterConfig#getGroup()}
     * @param tag   {@link LimiterConfig#getTag()} ()}
     */
    public LimiterConfig getLimiterConfig(String group, String tag) {
        return rules.containsKey(group) ? rules.get(group).get(tag) : null;
    }

    /**
     * The add limiter
     *
     * @param limiterConfig {@link LimiterConfig}
     */
    public void addLimiter(LimiterConfig limiterConfig) {
        ILimiter limiter = ExtensionLoader.getLoader(ILimiter.class).getExtension(limiterConfig.getMode().getValue());
        limiter.addListener(new EventListener() {
            @Override
            public void onEvent(LimiterConfig limiterConfig, EventType eventType, Object... args) {

            }
        });
        limiters.put(limiterConfig.identity(), limiter);
        rules.computeIfAbsent(limiterConfig.getGroup(), k -> new ConcurrentHashMap<>())
                .put(limiterConfig.getTag(), limiterConfig);
    }

    /**
     * The notify of changed config
     *
     * @param limiterConfig {@link LimiterConfig}
     * @throws Exception exception
     */
    public void notify(LimiterConfig limiterConfig) throws Exception {
        ILimiter limiter = limiters.get(limiterConfig.identity());
        if (null == limiter) {
            log.warn("Notfound limiter, identity={}", limiterConfig.identity());
            return;
        }
        if (limiter.refresh(limiterConfig)) {
            log.info("The limiter config refresh success: {}", limiterConfig);
        } else {
            log.warn("The limiter config refresh failure: {}", limiterConfig);
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

            ILimiter limiter = limiters.get(identity);
            limiterContext.setLimiterConfig(limiter.getConfig());
            return limiter.wrapperCall(limiterContext, originalCall);
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
                Map<String, Long> tempDataMap = limiter.collect();
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
                Map<String, Long> tempDataMap = limiter.statistics();
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
