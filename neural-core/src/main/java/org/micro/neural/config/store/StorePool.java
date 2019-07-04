package org.micro.neural.config.store;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.Neural;
import org.micro.neural.common.Constants;
import org.micro.neural.common.URL;
import org.micro.neural.common.collection.ConcurrentHashSet;

import static org.micro.neural.common.Constants.*;

import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.*;
import org.micro.neural.config.GlobalConfig.*;
import org.micro.neural.extension.ExtensionLoader;

import java.util.*;
import java.util.concurrent.*;

/**
 * Store Pool
 * <p>
 * space=neural
 * module=limiter/degrade
 * <p>
 * 1.GlobalConfig-Hash: [space]:GLOBAL-->[module]-->[json]
 * 2.NeuralConfig-Hash: [space]:RULE-->[module]:[application]:[group]:[resource]-->[json]
 * 3.GlobalStatistics-Hash: [space]:STATISTICS-->[module]:[application]:[group]:[resource]-->[json]
 * <p>
 * 1.GlobalConfig-Channel: [space]:GLOBAL:CHANNEL:[module]-->[json]
 * 2.NeuralConfig-Channel: [space]:RULE:CHANNEL:[module]:[application]:[group]:[resource]-->[json]
 * <p>
 * identity=[module]
 * identity=[module]:[application]:[group]:[resource]
 *
 * @author lry
 */
@Slf4j
public class StorePool implements IStoreListener {

    private static final String SPACE_DEFAULT = "neural";
    private static final String PULL_CONFIG_CYCLE_KEY = "pullConfigCycle";
    private static final String STATISTIC_REPORT_CYCLE_KEY = "statisticReportCycle";

    private boolean started;

    private String space;
    private long pullConfigCycle;
    private long statisticReportCycle;
    private IStore store;

    private ScheduledExecutorService pullConfigExecutor = null;
    private ScheduledExecutorService pushStatisticsExecutor = null;

    private volatile Set<String> channels = new ConcurrentHashSet<>();
    /**
     * Map<[module], Neural>
     */
    private volatile Map<String, Neural> modules = new ConcurrentHashMap<>();
    /**
     * Map<[module], Map<[identity], [JSON]>>
     */
    private volatile Map<String, Map<String, String>> ruleConfigs = new ConcurrentHashMap<>();

    private static StorePool INSTANCE = new StorePool();

    private StorePool() {
    }

    public static StorePool getInstance() {
        return INSTANCE;
    }

    public void register(String module, Neural neural) {
        modules.put(module, neural);
    }

    public void register(String module, String identity, String ruleConfig) {
        Map<String, String> moduleMap = ruleConfigs.computeIfAbsent(module, k -> new HashMap<>());
        moduleMap.put(identity, ruleConfig);
    }

    public IStore getStore() {
        return store;
    }

    public synchronized void initialize(URL url) {
        if (started) {
            return;
        }

        this.started = true;
        this.pullConfigCycle = url.getParameter(PULL_CONFIG_CYCLE_KEY, 5L);
        this.statisticReportCycle = url.getParameter(STATISTIC_REPORT_CYCLE_KEY, 1000L);
        this.space = url.getParameter(URL.GROUP_KEY, SPACE_DEFAULT);
        if (space.contains(Constants.DELIMITER)) {
            throw new IllegalArgumentException("The space can't include ':'");
        }
        space = space.toUpperCase();

        this.store = ExtensionLoader.getLoader(IStore.class).getExtension(url.getProtocol());
        store.initialize(url);

        // start cycle pull configs scheduled
        scheduledPullConfigs();
        // start subscribe configs listener
        subscribeNotifyConfigs();
        // start cycle push statistics scheduled
        scheduledPushStatistics();

        // add shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
    }

    /**
     * The subscribe
     *
     * @param module module
     * @param object {@link RuleConfig}
     */
    public void publish(String module, Object object) {
        String channel;
        if (object instanceof GlobalConfig) {
            channel = space + DELIMITER + Category.GLOBAL.name() + DELIMITER + module;
        } else if (object instanceof RuleConfig) {
            RuleConfig ruleConfig = (RuleConfig) object;
            channel = space + DELIMITER + Category.RULE.name() +
                    DELIMITER + module + DELIMITER + ruleConfig.identity();
        } else {
            throw new IllegalArgumentException("Illegal object type");
        }
        store.publish(channel, object);
    }

    /**
     * The cycle pull configs scheduled
     */
    private void scheduledPullConfigs() {
        if (null != pullConfigExecutor) {
            log.warn("The {} cyclePullConfigs is executed", space);
            return;
        }

        // initialize pull all configs
        pullConfigs();

        // start pull config data executor
        log.debug("The {} executing pull config data executor", space);

        // build Task Name
        ThreadFactoryBuilder pullBuilder = new ThreadFactoryBuilder();
        pullBuilder.setDaemon(true);
        pullBuilder.setNameFormat("neural-scheduled-pull-config");
        ThreadFactory pullThreadFactory = pullBuilder.build();
        this.pullConfigExecutor = Executors.newScheduledThreadPool(1, pullThreadFactory);

        // execute schedule pull config by fixed rate
        this.pullConfigExecutor.scheduleAtFixedRate(this::pullConfigs,
                pullConfigCycle, pullConfigCycle, TimeUnit.MILLISECONDS);
    }

    /**
     * The pull all configs
     */
    private void pullConfigs() {
        List<String> remoteChannels = new ArrayList<>();
        // pull remote global configs
        String remoteGlobalConfigKey = String.join(DELIMITER, space, Category.GLOBAL.name());
        Map<String, String> remoteGlobalConfigs = store.pull(remoteGlobalConfigKey);
        log.debug("The global config pull changed: {}", remoteGlobalConfigs);
        Map<String, String> addRemoteGlobalConfigs = new HashMap<>(modules.size());
        for (Map.Entry<String, Neural> entry : modules.entrySet()) {
            String module = entry.getKey().toUpperCase();
            if (remoteGlobalConfigs.isEmpty() || !remoteGlobalConfigs.containsKey(module)) {
                addRemoteGlobalConfigs.put(module, SerializeUtils.serialize(entry.getValue().getGlobalConfig()));
            }
        }
        if (!addRemoteGlobalConfigs.isEmpty()) {
            store.batchAdd(remoteGlobalConfigKey, addRemoteGlobalConfigs);
            remoteGlobalConfigs.putAll(addRemoteGlobalConfigs);
        }
        for (Map.Entry<String, String> entry : remoteGlobalConfigs.entrySet()) {
            String remoteGlobalChannel = String.join(DELIMITER, remoteGlobalConfigKey, CHANNEL, entry.getKey());
            remoteChannels.add(remoteGlobalChannel);
            Neural neural = modules.get(entry.getKey());
            if (neural != null) {
                neural.notify(Category.GLOBAL, Category.GLOBAL.name(), entry.getValue());
            }
        }

        // pull remote rule configs
        String remoteRuleConfigKey = String.join(DELIMITER, space, Category.RULE.name());
        Map<String, String> remoteRuleConfigs = store.pull(remoteRuleConfigKey);
        log.debug("The rule config pull changed: {}", remoteRuleConfigs);
        Map<String, String> addRemoteRuleConfigs = new HashMap<>(modules.size());
        for (Map.Entry<String, Map<String, String>> entry : ruleConfigs.entrySet()) {
            for (Map.Entry<String, String> subEntry : entry.getValue().entrySet()) {
                String identity = subEntry.getKey();
                if (remoteRuleConfigs.isEmpty() || !remoteRuleConfigs.containsKey(identity)) {
                    addRemoteRuleConfigs.put(identity, subEntry.getValue());
                }
            }
        }
        if (!addRemoteRuleConfigs.isEmpty()) {
            store.batchAdd(remoteRuleConfigKey, addRemoteRuleConfigs);
            remoteRuleConfigs.putAll(addRemoteRuleConfigs);
        }
        for (Map.Entry<String, String> entry : remoteRuleConfigs.entrySet()) {
            String remoteRuleChannel = String.join(DELIMITER, remoteRuleConfigKey, CHANNEL, entry.getKey());
            remoteChannels.add(remoteRuleChannel);
            String identity = entry.getKey();
            Neural neural = modules.get(identity.substring(0, identity.indexOf(DELIMITER)));
            if (neural != null) {
                neural.notify(Category.RULE, identity, entry.getValue());
            }
        }

        // update channel list
        channels.clear();
        if (!remoteChannels.isEmpty()) {
            channels.addAll(remoteChannels);
        }
    }

    /**
     * The subscribe configs
     */
    private void subscribeNotifyConfigs() {
        // start subscribe config data executor
        log.debug("The {} executing subscribe config data executor", space);
        if (channels.isEmpty()) {
            return;
        }

        // the execute subscribe
        store.subscribe(channels, this);
    }

    @Override
    public void notify(String channel, String data) {
        log.debug("The {} config subscribed changed: {}, {}", space, channel, data);
        if (null == channel || channel.length() == 0 || null == data || data.length() == 0) {
            return;
        }

        Category remoteCategory = Category.valueOf(channel.split(DELIMITER)[0]);
        String remoteChannel = channel.substring(channel.indexOf(CHANNEL) + 8);
        String module = remoteChannel.split(DELIMITER)[0];
        Neural neural = modules.get(module);
        if (neural != null) {
            String identity = null;
            if (Category.RULE == remoteCategory) {
                identity = remoteChannel.substring(remoteChannel.indexOf(DELIMITER) + 1);
            }
            neural.notify(remoteCategory, identity, data);
        }
    }

    /**
     * The cycle push statistics scheduled
     */
    private void scheduledPushStatistics() {
        if (null != pushStatisticsExecutor) {
            log.warn("The {} cyclePushStatistics is executed", space);
            return;
        }

        // start push statistics data executor
        log.debug("The {} executing push statistics data executor", space);
        ThreadFactoryBuilder pushBuilder = new ThreadFactoryBuilder();
        ThreadFactory pushTreadFactory = pushBuilder.setDaemon(true).setNameFormat(space + "-push-statistics").build();
        this.pushStatisticsExecutor = Executors.newScheduledThreadPool(1, pushTreadFactory);

        // execute schedule push statistics by fixed rate
        this.pushStatisticsExecutor.scheduleAtFixedRate(this::collect,
                statisticReportCycle, statisticReportCycle, TimeUnit.MILLISECONDS);
    }

    /**
     * The collect
     */
    @SuppressWarnings("unchecked")
    private void collect() {
        NodeConfig nodeConfig = new NodeConfig();
        for (Map.Entry<String, Neural> entry : modules.entrySet()) {
            try {
                Neural neural = entry.getValue();
                GlobalConfig globalConfig = neural.getGlobalConfig();
                long time = buildStatisticsTime(globalConfig.getStatisticReportCycle());

                // query memory statistics data
                Map<String, Map<String, Long>> statisticsData = neural.collect();
                log.debug("The {} cycle push statistics: {}", space, statisticsData);
                if (null == statisticsData || statisticsData.isEmpty()) {
                    return;
                }

                System.out.println(statisticsData);
                for (Map.Entry<String, Map<String, Long>> identityEntry : statisticsData.entrySet()) {
                    Map<String, Object> sendData = new HashMap<>(JSON.parseObject(JSON.toJSONString(nodeConfig), Map.class));
                    for (Map.Entry<String, Long> tempEntry : identityEntry.getValue().entrySet()) {
                        sendData.put(tempEntry.getKey(), tempEntry.getValue());
                    }

                    String key = String.join(DELIMITER, space, STATISTICS, identityEntry.getKey(), String.valueOf(time));
                    // push statistics data to remote
                    store.batchIncrementBy(key, sendData, neural.getGlobalConfig().getStatisticExpire());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * The build statistics time
     *
     * @param statisticReportCycle statistic report cycle milliseconds
     * @return statistics time
     */
    private long buildStatisticsTime(long statisticReportCycle) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int second = (int) statisticReportCycle / 1000;
        int tempSecond = calendar.get(Calendar.SECOND) % second;
        second = tempSecond >= second / 2 ? second : 0;
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + second - tempSecond);

        return calendar.getTimeInMillis();
    }

    /**
     * The destroy store config
     */
    public void destroy() {
        log.debug("The {} is executing destroy", space);
        if (null != pullConfigExecutor) {
            pullConfigExecutor.shutdown();
        }
        if (null != pushStatisticsExecutor) {
            pushStatisticsExecutor.shutdown();
        }

        store.unsubscribe(this);
        if (null != store) {
            store.destroy();
        }
    }

}
