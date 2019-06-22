package org.micro.neural.config.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.URL;
import org.micro.neural.config.store.StorePool;

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

    /**
     * The initialize
     */
    public void initialize(URL url) {
        log.debug("The starting of event");

        // parse parameters
        EventConfig eventConfig = url.getObj(EventConfig.class);
        // build thread pool
        this.eventExecutor = buildExecutorService(eventConfig);
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
                if (storePool.getStore() != null) {
                    storePool.getStore();
                }
                log.info("The module[{}],eventType[{}], args[{}]", module, eventType, args);
            } catch (Exception e) {
                log.error("The module[" + module + "] eventType[" + eventType + "] is exception", e);
            }
        });
    }

    /**
     * The build ExecutorService
     *
     * @param eventConfig {@link EventConfig}
     * @return {@link ExecutorService}
     */
    private ExecutorService buildExecutorService(EventConfig eventConfig) {
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

}
