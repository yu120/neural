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

    public static final String EVENT_THREAD_KEY = "event-thread";
    public static final String EVENT_CAPACITY_KEY = "event-capacity";

    private StorePool storePool = StorePool.getInstance();
    private ExecutorService eventExecutor = null;

    /**
     * The initialize
     */
    public void initialize(URL url) {
        log.debug("The starting of event");

        // parse parameters
        int eventThread = url.getParameter(EVENT_THREAD_KEY, 1);
        int eventCapacity = url.getParameter(EVENT_CAPACITY_KEY, 1000);
        EventRejectedStrategy eventRejectedStrategy = url.getParameter(
                EVENT_CAPACITY_KEY, EventRejectedStrategy.DISCARD_OLDEST_POLICY);

        // build thread pool
        ThreadFactoryBuilder subscribeBuilder = new ThreadFactoryBuilder();
        subscribeBuilder.setDaemon(true);
        subscribeBuilder.setNameFormat("neural-event-processor");
        ThreadFactory subscribeThreadFactory = subscribeBuilder.build();
        this.eventExecutor = new ThreadPoolExecutor(
                eventThread, eventThread, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(eventCapacity),
                subscribeThreadFactory, eventRejectedStrategy.getStrategy());

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
     * The destroy
     */
    public void destroy() {
        log.debug("The destroy of event");
        if (null != eventExecutor) {
            eventExecutor.shutdown();
        }
    }

}
