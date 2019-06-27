package org.micro.neural.config.store;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.Neural;
import org.micro.neural.common.URL;
import org.micro.neural.common.collection.ConcurrentHashSet;

import static org.micro.neural.common.Constants.*;

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
 * 1.GlobalConfig-Hash: GLOBAL:[space]-->[module]-->[json]
 * 2.NeuralConfig-Hash: RULE:[space]-->[module]:[application]:[group]:[resource]-->[json]
 * <p>
 * 3.GlobalConfig-Channel: GLOBAL:[space]:CHANNEL:[module]-->[message]
 * 4.NeuralConfig-Channel: RULE:[space]:CHANNEL:[module]:[application]:[group]:[resource]-->[message]
 * <p>
 * identity=GLOBAL
 * identity=RULE:[application]:[group]:[resource]
 *
 * @author lry
 */
@Slf4j
public class StorePool implements IStoreListener {

    public static final String SPACE_DEFAULT = "neural";
    public static final String PULL_CONFIG_CYCLE_KEY = "pullConfigCycle";
    public static final String STATISTIC_REPORT_CYCLE_KEY = "statisticReportCycle";

    private String space;
    private long pullConfigCycle;
    private long statisticReportCycle;
    private IStore store;

    private ScheduledExecutorService pullConfigExecutor = null;
    private ScheduledExecutorService pushStatisticsExecutor = null;

    private volatile Set<String> channels = new ConcurrentHashSet<>();
    /**
     * Map<[module], IStoreListener>
     */
    private volatile Map<String, Neural> neuralMap = new ConcurrentHashMap<>();
    private volatile Map<String, String> globalConfigs = new ConcurrentHashMap<>();

    private static StorePool INSTANCE = new StorePool();

    private StorePool() {
    }

    public static StorePool getInstance() {
        return INSTANCE;
    }

    public void register(String module, Neural neural, String globalConfig) {
        neuralMap.put(module, neural);
        globalConfigs.put(module, globalConfig);
    }

    public IStore getStore() {
        return store;
    }

    public void initialize(URL url) {
        this.pullConfigCycle = url.getParameter(PULL_CONFIG_CYCLE_KEY, 5L);
        this.statisticReportCycle = url.getParameter(STATISTIC_REPORT_CYCLE_KEY, 5000L);
        this.space = url.getParameter(URL.GROUP_KEY);
        if (space == null || space.length() == 0) {
            space = SPACE_DEFAULT;
        }
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
            channel = Category.GLOBAL.name() + DELIMITER + space + DELIMITER + module;
        } else if (object instanceof RuleConfig) {
            RuleConfig ruleConfig = (RuleConfig) object;
            channel = Category.RULE.name() + DELIMITER + space +
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
        String remoteGlobalConfigKey = String.join(DELIMITER, Category.GLOBAL.name(), space);
        Map<String, String> remoteGlobalConfigs = store.pull(remoteGlobalConfigKey);
        log.debug("The global config pull changed: {}", remoteGlobalConfigs);
        if (remoteGlobalConfigs == null || remoteGlobalConfigs.isEmpty()) {
            if (!globalConfigs.isEmpty()) {
                store.batchAdd(remoteGlobalConfigKey, globalConfigs);
                remoteGlobalConfigs = new HashMap<>(globalConfigs);
            }
        }
        if (remoteGlobalConfigs != null && !remoteGlobalConfigs.isEmpty()) {
            for (Map.Entry<String, String> entry : remoteGlobalConfigs.entrySet()) {
                String remoteGlobalChannel = String.join(DELIMITER, remoteGlobalConfigKey, CHANNEL, entry.getKey());
                remoteChannels.add(remoteGlobalChannel);

                String module = entry.getKey();
                Neural neural = neuralMap.get(module);
                if (neural != null) {
                    neural.notify(Category.GLOBAL, Category.GLOBAL.name(), entry.getValue());
                }
            }
        }

        // pull remote rule configs
        String remoteRuleConfigKey = String.join(DELIMITER, Category.RULE.name(), space);
        Map<String, String> remoteRuleConfigs = store.pull(remoteRuleConfigKey);
        log.debug("The rule config pull changed: {}", remoteRuleConfigs);
        if (remoteRuleConfigs != null && !remoteRuleConfigs.isEmpty()) {
            for (Map.Entry<String, String> entry : remoteRuleConfigs.entrySet()) {
                String remoteRuleChannel = String.join(DELIMITER, remoteRuleConfigKey, CHANNEL, entry.getKey());
                remoteChannels.add(remoteRuleChannel);

                int index = entry.getKey().indexOf(DELIMITER);
                String module = entry.getKey().substring(0, index);
                Neural neural = neuralMap.get(module);
                if (neural != null) {
                    String identity = entry.getKey().substring(index + 1);
                    neural.notify(Category.RULE, identity, entry.getValue());
                }
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
        Neural neural = neuralMap.get(module);
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
    @SuppressWarnings("unchecked")
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
        this.pushStatisticsExecutor.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Neural> entry : neuralMap.entrySet()) {
                try {
                    Neural neural = entry.getValue();

                    // query memory statistics data
                    Map<String, Long> statisticsData = neural.collect();
                    log.debug("The {} cycle push statistics: {}", space, statisticsData);
                    if (null == statisticsData || statisticsData.isEmpty()) {
                        return;
                    }

                    Map<String, Long> sendData = new HashMap<>();
                    for (Map.Entry<String, Long> tempEntry : statisticsData.entrySet()) {
                        sendData.put(String.join(DELIMITER, space, tempEntry.getKey()), tempEntry.getValue());
                    }

                    // push statistics data to remote
                    store.batchIncrBy(neural.getGlobalConfig().getStatisticExpire(), sendData);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, statisticReportCycle, statisticReportCycle, TimeUnit.MILLISECONDS);
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

        store.unSubscribe(this);
        if (null != store) {
            store.destroy();
        }
    }

}
