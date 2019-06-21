package org.micro.neural.degrade;

import org.micro.neural.common.Constants;
import org.micro.neural.event.EventProcessor;
import org.micro.neural.common.utils.SerializeUtils;
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
@Extension("degrade")
public class Degrade extends AbstractNeural<DegradeConfig, DegradeGlobalConfig> {

    private volatile Map<String, Object> mockDataMap = new HashMap<>();
    private final ConcurrentMap<String, DegradeStatistics> degradeStatistics = new ConcurrentHashMap<>();

    @Override
    public void addConfig(DegradeConfig config) {
        super.addConfig(config);
        degradeStatistics.put(config.identity(), new DegradeStatistics());
        mockDataMap.put(config.identity(), mockData(config.getMock(), config.getClazz(), config.getData()));
    }

    @Override
    public Object doWrapperCall(String identity, OriginalCall originalCall) throws Throwable {
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

        // total request traffic
        degradeStatistics.get(identity).totalRequestTraffic();

        // the check degrade enable, Switch.OFF is opened if degrade
        if (Switch.OFF == degradeConfig.getEnable()) {
            return doDegradeCallWrapper(identity, degradeConfig.getStrategy(), originalCall);
        }

        // the check Config.Level.order <= GlobalConfig.Level.order<=
        if (degradeConfig.getLevel().getOrder() <= globalConfig.getLevel().getOrder()) {
            return doDegradeCallWrapper(identity, degradeConfig.getStrategy(), originalCall);
        }

        return doOriginalCallWrapper(identity, originalCall);
    }

    @Override
    public Map<String, Long> collect() {
        Map<String, Long> dataMap = new HashMap<>();
        try {
            degradeStatistics.forEach((identity, statistics) -> {
                Map<String, Long> temp = statistics.getAndReset(identity, globalConfig.getStatisticReportCycle());
                if (null == temp || temp.isEmpty()) {
                    return;
                }

                dataMap.putAll(temp);
            });
        } catch (Exception e) {
            EventProcessor.EVENT.notify(module, EventType.COLLECT_EXCEPTION);
            log.error("The notify config is exception of degrade", e);
        }

        return dataMap;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void doNotify(String identity, DegradeConfig ruleConfig) {
        try {
            Object mockData = mockData(ruleConfig.getMock(), ruleConfig.getClazz(), ruleConfig.getData());
            mockDataMap.put(identity, mockData);
        } catch (Exception e) {
            EventProcessor.EVENT.notify(module, EventType.NOTIFY_EXCEPTION);
            log.error("The collect statistics is exception of degrade", e);
        }
    }

    /**
     * The do original call wrapper
     *
     * @param identity     {@link org.micro.neural.config.RuleConfig}
     * @param originalCall {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    private Object doOriginalCallWrapper(String identity, OriginalCall originalCall) throws Throwable {
        DegradeStatistics statistics = degradeStatistics.get(identity);

        // increment traffic
        statistics.incrementTraffic();
        long startTime = System.currentTimeMillis();

        try {
            return originalCall.call();
        } catch (Throwable t) {
            // total exception traffic
            statistics.exceptionTraffic(t);
            throw t;
        } finally {
            // decrement traffic
            statistics.decrementTraffic(startTime);
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

    /**
     * The get mock data
     *
     * @param mock  {@link Mock}
     * @param clazz class name
     * @param data  data
     * @return mock data
     */
    private Object mockData(Mock mock, String clazz, String data) {
        switch (mock) {
            case NULL:
                return null;
            case STRING:
                return String.valueOf(data);
            case INTEGER:
                return Integer.valueOf(data);
            case FLOAT:
                return Float.valueOf(data);
            case DOUBLE:
                return Double.valueOf(data);
            case LONG:
                return Long.valueOf(data);
            case BOOLEAN:
                return Boolean.valueOf(data);
            case ARRAY:
                return data.split(Constants.SEPARATOR);
            case CLASS:
                return SerializeUtils.deserialize(newClass(clazz), data);
            case MAP:
                return SerializeUtils.parseMap(data);
            case MAP_STR:
                return SerializeUtils.parseStringMap(data);
            case MAP_OBJ:
                return SerializeUtils.parseObjMap(data);
            case LIST:
                return SerializeUtils.parseList(data);
            case LIST_STR:
                return SerializeUtils.parseListString(data);
            case LIST_CLASS:
                return SerializeUtils.deserialize(newClass(clazz), data);
            default:
                throw new IllegalArgumentException("The illegal mock: " + mock);
        }
    }

    private Class<?> newClass(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The illegal class type: " + clazz);
        }
    }

}
