package cn.micro.neural.idempotent.core;

import cn.micro.neural.idempotent.IdempotentConfig;
import cn.micro.neural.idempotent.IdempotentStatistics;
import cn.micro.neural.idempotent.event.EventListener;
import cn.micro.neural.idempotent.event.EventType;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.utils.BeanUtils;
import cn.neural.common.utils.CloneUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AbstractCircuitBreaker
 *
 * @author lry
 */
@Slf4j
@Getter
public abstract class AbstractIdempotent implements Idempotent {

    private final Set<EventListener> listeners = new LinkedHashSet<>();
    private final IdempotentStatistics statistics = new IdempotentStatistics();
    protected IdempotentConfig config;

    public AbstractIdempotent(IdempotentConfig config) {
        this.config = config;
    }

    @Override
    public void addListener(EventListener... eventListeners) {
        Collections.addAll(listeners, eventListeners);
    }

    @Override
    public synchronized boolean refresh(IdempotentConfig config) throws Exception {
        try {
            log.info("Refresh the current circuit-breaker config: {}", config);
            if (null == config || this.config.equals(config)) {
                return false;
            }

            // Copy properties attributes after deep copy
            BeanUtils.copyProperties(CloneUtils.clone(config), this.config);

            return tryRefresh(config);
        } catch (Exception e) {
            this.collectEvent(EventType.REFRESH_EXCEPTION, config);
            throw e;
        }
    }

    @Override
    public Object wrapperCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        OriginalContext.set(originalContext);

        try {
            return originalCall.call(originalContext);
        } finally {
            OriginalContext.remove();
        }
    }

    @Override
    public Map<String, Long> collect() {
        try {
            return statistics.collectThenReset();
        } catch (Exception e) {
            this.collectEvent(EventType.COLLECT_EXCEPTION);
            log.error("The limiter[{}] collect exception", config.identity(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * The collect event
     *
     * @param eventType {@link EventType}
     * @param args      attachment parameters
     */
    private void collectEvent(EventType eventType, Object... args) {
        for (EventListener eventListener : listeners) {
            try {
                eventListener.onEvent(config, eventType, args);
            } catch (Exception e) {
                log.error("The collect event exception", e);
            }
        }
    }

    /**
     * The do refresh
     *
     * @param config {@link IdempotentConfig}
     * @return true is success
     */
    protected abstract boolean tryRefresh(IdempotentConfig config);

}
