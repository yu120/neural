package org.micro.neural.limiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.micro.neural.event.EventProcessor;
import org.micro.neural.config.GlobalConfig.*;
import org.micro.neural.OriginalCall;
import org.micro.neural.AbstractNeural;
import org.micro.neural.extension.Extension;
import org.micro.neural.extension.ExtensionLoader;
import org.micro.neural.limiter.core.ILimiter;
import org.micro.neural.limiter.LimiterGlobalConfig.EventType;
import lombok.extern.slf4j.Slf4j;

/**
 * The Limiter.
 *
 * @author lry
 **/
@Slf4j
@Extension("limiter")
public class Limiter extends AbstractNeural<LimiterConfig, LimiterGlobalConfig> {

    private final ConcurrentMap<String, ILimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public void addConfig(LimiterConfig config) {
        super.addConfig(config);
        limiters.put(config.identity(), ExtensionLoader.getLoader(ILimiter.class).getExtension());
    }

    @Override
    public Object doWrapperCall(String identity, OriginalCall originalCall) throws Throwable {
        // The check global config of limiter
        if (null == globalConfig || null == globalConfig.getEnable() || Switch.OFF == globalConfig.getEnable()) {
            return originalCall.call();
        }

        // The check limiter object
        if (null == identity || !limiters.containsKey(identity)) {
            return originalCall.call();
        }

        return limiters.get(identity).doOriginalCall(originalCall);
    }

    @Override
    public Map<String, Long> collect() {
        Map<String, Long> dataMap = super.collect();
        try {
            limiters.forEach((identity, limiter) -> {
                Map<String, Long> tempDataMap = limiter.getStatistics().getAndReset(
                        identity, globalConfig.getStatisticReportCycle());
                if (null == tempDataMap || tempDataMap.isEmpty()) {
                    return;
                }

                dataMap.putAll(tempDataMap);
            });
        } catch (Exception e) {
            EventProcessor.EVENT.notify(module, EventType.COLLECT_EXCEPTION);
            log.error(EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

    @Override
    public Map<String, Long> statistics() {
        Map<String, Long> dataMap = super.collect();
        try {
            limiters.forEach((identity, limiter) -> {
                Map<String, Long> tempDataMap = limiter.getStatistics().getStatisticsData();
                if (null == tempDataMap || tempDataMap.isEmpty()) {
                    return;
                }

                dataMap.putAll(tempDataMap);
            });
        } catch (Exception e) {
            EventProcessor.EVENT.notify(module, EventType.COLLECT_EXCEPTION);
            log.error(EventType.COLLECT_EXCEPTION.getMessage(), e);
        }

        return dataMap;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (!limiters.isEmpty()) {
            limiters.values().forEach(ILimiter::destroy);
        }
    }

    @Override
    protected void doNotify(String identity, LimiterConfig ruleConfig) {
        try {
            ILimiter limiter = limiters.get(identity);
            if (null == limiter) {
                log.warn("The limiter config is notify is exception, not found limiter:[{}]", identity);
                return;
            }

            limiter.refresh(ruleConfig);
        } catch (Exception e) {
            EventProcessor.EVENT.notify(module, EventType.NOTIFY_EXCEPTION);
            log.error(EventType.NOTIFY_EXCEPTION.getMessage(), e);
        }
    }

}
