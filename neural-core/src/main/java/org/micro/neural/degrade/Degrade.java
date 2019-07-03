package org.micro.neural.degrade;

import org.micro.neural.NeuralContext;
import org.micro.neural.config.event.EventCollect;
import org.micro.neural.config.GlobalConfig.*;
import org.micro.neural.OriginalCall;
import org.micro.neural.AbstractNeural;
import org.micro.neural.degrade.DegradeConfig.*;
import org.micro.neural.degrade.DegradeGlobalConfig.EventType;
import org.micro.neural.extension.Extension;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Degrade.
 *
 * @author lry
 */
@Slf4j
@Extension(DegradeGlobalConfig.IDENTITY)
public class Degrade extends AbstractNeural<DegradeConfig, DegradeGlobalConfig> {

    private volatile Map<String, Object> mockDataMap = new HashMap<>();
    private final ConcurrentMap<String, DegradeStatistics> degradeStatistics = new ConcurrentHashMap<>();

    @Override
    public void addConfig(DegradeConfig config) {
        super.addConfig(config);
        degradeStatistics.put(config.identity(), new DegradeStatistics());
        mockDataMap.put(config.identity(), config.getMock().getFunction().apply(config.getData(), config.getClazz()));
    }

    @Override
    public Object wrapperCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        // the check global config of degrade
        if (null == globalConfig || null == globalConfig.getEnable() || Switch.OFF == globalConfig.getEnable()) {
            return originalCall.call();
        }

        // the check degrade object
        if (null == identity || !configs.containsKey(identity)) {
            return originalCall.call();
        }

        DegradeConfig degradeConfig = configs.get(identity);
        // the check config and degrade enable is null
        if (null == degradeConfig || null == degradeConfig.getEnable()) {
            return originalCall.call();
        }

        // the check degrade level
        if (null == degradeConfig.getLevel() || null == globalConfig.getLevel()) {
            return originalCall.call();
        }

        // the check degrade enable, Switch.OFF is opened if degrade
        if (Switch.OFF == degradeConfig.getEnable()) {
            return doDegradeCallWrapper(identity, degradeConfig.getStrategy(), originalCall);
        }

        // the check Config.Level.order <= GlobalConfig.Level.order<=
        if (degradeConfig.getLevel().getOrder() <= globalConfig.getLevel().getOrder()) {
            return doDegradeCallWrapper(identity, degradeConfig.getStrategy(), originalCall);
        }

        // the wrapper of original call
        DegradeStatistics statistics = degradeStatistics.get(identity);
        if (statistics == null) {
            return originalCall.call();
        }

        return statistics.wrapperOriginalCall(neuralContext, originalCall);
    }

    @Override
    public Map<String, Map<String, Long>> collect() {
        Map<String, Map<String, Long>> dataMap = new HashMap<>();
        try {
            degradeStatistics.forEach((identity, statistics) -> {
                Map<String, Long> temp = statistics.getAndReset();
                if (null == temp || temp.isEmpty()) {
                    return;
                }

                dataMap.put(identity, temp);
            });
        } catch (Exception e) {
            EventCollect.onEvent(EventType.COLLECT_EXCEPTION);
            log.error("The notify config is exception of degrade", e);
        }

        return dataMap;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void ruleNotify(String identity, DegradeConfig ruleConfig) {
        super.ruleNotify(identity, ruleConfig);

        try {
            Object mockData = ruleConfig.getMock().getFunction().apply(ruleConfig.getData(), ruleConfig.getClazz());
            mockDataMap.put(identity, mockData);
        } catch (Exception e) {
            EventCollect.onEvent(EventType.NOTIFY_EXCEPTION);
            log.error("The collect statistics is exception of degrade", e);
        }
    }

    /**
     * The do degrade call wrapper
     *
     * @param identity    {@link org.micro.neural.config.RuleConfig}
     * @param strategy    {@link Strategy}
     * @param degradeCall {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    private Object doDegradeCallWrapper(String identity, Strategy strategy, OriginalCall degradeCall) throws Throwable {
        // degrade traffic
        degradeStatistics.get(identity).getCounter().increment();
        // degrade strategy
        switch (strategy) {
            case FALLBACK:
                return degradeCall.fallback();
            case MOCK:
                return mockDataMap.get(identity);
            case NON:
            default:
        }

        // no degrade traffic
        return degradeCall.call();
    }

}
