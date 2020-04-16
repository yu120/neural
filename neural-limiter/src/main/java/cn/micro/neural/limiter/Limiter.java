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
@Extension(LimiterGlobalConfig.IDENTITY)
public class Limiter {

    private LimiterGlobalConfig globalConfig;

    private final ConcurrentMap<String, ILimiter> limiters = new ConcurrentHashMap<>();

    public void addConfig(LimiterConfig config) {
        ILimiter limiter = ExtensionLoader.getLoader(ILimiter.class).getExtension(config.getModel());
        limiters.put(config.identity(), limiter);
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
     * @param identity      {@link LimiterConfig#identity()}
     * @param originalCall  {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    public Object originalCall(LimiterContext limiterContext, String identity, OriginalCall originalCall) throws Throwable {
        try {
            LimiterContext.set(limiterContext);

            // The check global config of limiter
            if (null == globalConfig || null == globalConfig.getEnable() ||
                    LimiterGlobalConfig.Switch.OFF == globalConfig.getEnable()) {
                return originalCall.call();
            }

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
        Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
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
            log.error(LimiterGlobalConfig.EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

    /**
     * The get statistics data
     *
     * @return statistics data
     */
    public Map<String, Map<String, Long>> statistics() {
        Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
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
            log.error(LimiterGlobalConfig.EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

    /**
     * The notify of changed config
     *
     * @param ruleConfig {@link LimiterConfig}
     */
    public void notifyGlobalConfig(LimiterGlobalConfig ruleConfig) {

    }

    /**
     * The notify of changed config
     *
     * @param identity   {@link LimiterConfig#identity()}
     * @param ruleConfig {@link LimiterConfig}
     */
    public void notifyRuleConfig(String identity, LimiterConfig ruleConfig) {
        try {
            ILimiter limiter = limiters.get(identity);
            if (null == limiter) {
                log.warn("The limiter config is notify is exception, not found limiter:[{}]", identity);
                return;
            }

            boolean flag = limiter.refresh(ruleConfig);
            if (!flag) {
                log.warn("The limiter refresh failure:{},{},{}", identity, globalConfig, ruleConfig);
            }
        } catch (Exception e) {
            //EventCollect.onEvent(LimiterGlobalConfig.EventType.NOTIFY_EXCEPTION);
            log.error(LimiterGlobalConfig.EventType.NOTIFY_EXCEPTION.getMessage(), e);
        }
    }

}
