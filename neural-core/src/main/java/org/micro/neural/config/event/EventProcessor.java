package org.micro.neural.config.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.store.StorePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * The Event Processor.
 *
 * @author lry
 **/
@Slf4j
public enum EventProcessor {

    // ====

    EVENT;

    private StorePool storePool = StorePool.getInstance();
    private ExecutorService eventExecutor = null;
    private EventConfig eventConfig;
    private Logger eventLog;

    /**
     * The initialize
     */
    public void initialize(URL url) {
        log.debug("The starting of event");

        // parse parameters
        this.eventConfig = url.getObj(EventConfig.class);
        // build event log
        this.eventLog = LoggerFactory.getLogger(eventConfig.getLogName());
        // build thread pool
        this.eventExecutor = buildExecutorService();
        // add shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
    }

    /**
     * The notify event
     *
     * @param module    module
     * @param eventType {@link IEventType}
     * @param args      args
     */
    public void notify(String module, IEventType eventType, Object... args) {
        if (eventExecutor == null) {
            return;
        }

        eventExecutor.submit(() -> {
            try {
                if (EventConfig.CollectStrategy.LOG == eventConfig.getCollectStrategy()) {
                    List<Object> argList = new ArrayList<>();
                    if (args != null && args.length > 0) {
                        argList = Arrays.asList(args);
                    }
                    System.out.println();

                    EventCollect eventCollect = new EventCollect(module, eventType.name(), argList);
                    eventLog.info("{}", eventConfig.isJsonLog() ?
                            SerializeUtils.serialize(eventCollect) : eventCollect.toString());
                } else if (EventConfig.CollectStrategy.REDIS == eventConfig.getCollectStrategy()) {
                    if (storePool.getStore() != null) {
                        //TODO
                    }
                }
            } catch (Exception e) {
                log.error("The module[" + module + "] eventType[" + eventType + "] is exception", e);
            }
        });
    }

    /**
     * The build ExecutorService
     *
     * @return {@link ExecutorService}
     */
    private ExecutorService buildExecutorService() {
        if (eventConfig == null) {
            return null;
        }

        // build thread pool
        ThreadFactoryBuilder subscribeBuilder = new ThreadFactoryBuilder();
        subscribeBuilder.setDaemon(true);
        subscribeBuilder.setNameFormat(eventConfig.getThreadName());
        ThreadFactory eventThreadFactory = subscribeBuilder.build();
        return new ThreadPoolExecutor(
                eventConfig.getCoreThread(),
                eventConfig.getMaxThread(),
                eventConfig.getKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(eventConfig.getCapacity()),
                eventThreadFactory,
                eventConfig.getRejectedStrategy().getStrategy());
    }

    /**
     * The destroy
     */
    public void destroy() {
        log.debug("The destroy of event");
        if (null != eventExecutor) {
            eventExecutor.shutdown();
        }
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCollect implements Serializable {
        private String module;
        private String eventType;
        private List<Object> args;
    }

}
