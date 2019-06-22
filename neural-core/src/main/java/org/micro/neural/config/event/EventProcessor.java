package org.micro.neural.config.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.URL;
import org.micro.neural.extension.Extension;
import org.micro.neural.extension.ExtensionLoader;

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

    private EventConfig eventConfig;
    private ExecutorService eventExecutor = null;
    private ConcurrentMap<String, IEventListener> listeners = new ConcurrentHashMap<>();

    /**
     * The initialize
     */
    public void initialize(URL url) {
        log.debug("The starting of event");

        // parse parameters
        this.eventConfig = url.getObj("event", EventConfig.class);

        // load event listener list
        List<IEventListener> eventListeners = ExtensionLoader.getLoader(IEventListener.class).getExtensions();
        for (IEventListener eventListener : eventListeners) {
            Extension extension = eventListener.getClass().getDeclaredAnnotation(Extension.class);
            if (extension != null) {
                eventListener.initialize(eventConfig);
                listeners.put(extension.value(), eventListener);
            }
        }

        // build thread pool
        this.eventExecutor = buildExecutorService();
        // add shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
    }

    /**
     * The notify event
     *
     * @param eventType {@link IEventType}
     * @param args      args
     */
    public void notify(IEventType eventType, Object... args) {
        if (eventExecutor == null || listeners.isEmpty()) {
            return;
        }

        eventExecutor.submit(() -> {
            try {
                IEventListener eventListener = listeners.get(eventConfig.getCollect());
                if (eventListener == null) {
                    return;
                }

                eventListener.notify(eventType, args);
            } catch (Exception e) {
                log.error("The module[" + eventType.getModule() + "],type[" + eventType.name() + "] is exception", e);
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
                eventConfig.getRejected().getStrategy());
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
