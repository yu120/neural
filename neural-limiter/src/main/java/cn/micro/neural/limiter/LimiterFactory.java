package cn.micro.neural.limiter;

import cn.micro.neural.limiter.core.ILimiter;
import cn.micro.neural.limiter.event.EventListener;
import cn.micro.neural.limiter.event.EventType;
import cn.neural.common.extension.Extension;
import cn.neural.common.extension.ExtensionLoader;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
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
public class LimiterFactory implements EventListener {

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
        LimiterConfig.Mode mode = limiterConfig.getMode();
        ILimiter limiter = ExtensionLoader.getLoader(ILimiter.class).getExtension(mode.getValue());
        limiter.addListener(this);

        limiters.put(limiterConfig.identity(), limiter);
        rules.computeIfAbsent(limiterConfig.getGroup(),
                k -> new ConcurrentHashMap<>()).put(limiterConfig.getTag(), limiterConfig);
    }

    /**
     * The check and add limiter
     *
     * @param limiterConfig {@link LimiterConfig}
     */
    public void checkAndAddLimiter(LimiterConfig limiterConfig) {
        if (!limiters.containsKey(limiterConfig.identity())) {
            addLimiter(limiterConfig);
        }
    }

    @Override
    public void onEvent(LimiterConfig limiterConfig, EventType eventType, Object... args) {
        log.info("Receive event[{}]", eventType);
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
            return;
        }

        log.warn("The limiter config refresh failure: {}", limiterConfig);
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
        return originalCall(new OriginalContext(), identity, originalCall);
    }

    /**
     * The process of original call
     *
     * @param originalContext {@link OriginalContext}
     * @param identity       {@link LimiterConfig#identity()}
     * @param originalCall   {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    public Object originalCall(final OriginalContext originalContext, String identity, OriginalCall originalCall) throws Throwable {
        try {
            OriginalContext.set(originalContext);
            // The check limiter object
            if (null == identity || !limiters.containsKey(identity)) {
                return originalCall.call(originalContext);
            }

            ILimiter limiter = limiters.get(identity);
            return limiter.wrapperCall(originalContext, originalCall);
        } finally {
            OriginalContext.remove();
        }
    }

    /**
     * The collect of get and reset statistics data
     *
     * @return statistics data
     */
    public Map<String, Map<String, Long>> collect() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        limiters.forEach((identity, limiter) -> {
            Map<String, Long> tempMap = limiter.collect();
            if (!tempMap.isEmpty()) {
                dataMap.put(identity, tempMap);
            }
        });

        return dataMap;
    }

    /**
     * The get statistics data
     *
     * @return statistics data
     */
    public Map<String, Map<String, Long>> statistics() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        limiters.forEach((identity, limiter) -> {
            Map<String, Long> tempMap = limiter.statistics();
            if (!tempMap.isEmpty()) {
                dataMap.put(identity, tempMap);
            }
        });

        return dataMap;
    }

}
