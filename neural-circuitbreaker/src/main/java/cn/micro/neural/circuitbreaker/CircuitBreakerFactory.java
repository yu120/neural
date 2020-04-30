package cn.micro.neural.circuitbreaker;

import cn.micro.neural.circuitbreaker.core.ICircuitBreaker;
import cn.micro.neural.circuitbreaker.event.EventListener;
import cn.micro.neural.circuitbreaker.event.EventType;
import cn.micro.neural.storage.Neural;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.extension.Extension;
import cn.neural.common.extension.ExtensionLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CircuitBreakerFactory
 *
 * @author lry
 **/
@Slf4j
@Getter
@Extension(CircuitBreakerFactory.IDENTITY)
public class CircuitBreakerFactory implements EventListener, Neural<CircuitBreakerConfig> {

    public static final String IDENTITY = "circuit_breaker";

    /**
     * Map<key=ICircuitBreaker#identity(), ICircuitBreaker>
     */
    private final ConcurrentMap<String, ICircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    /**
     * Map<key=CircuitBreakerConfig#getGroup(), subKey=CircuitBreakerConfig#getTag(), value=CircuitBreakerConfig>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, CircuitBreakerConfig>> rules = new ConcurrentHashMap<>();

    @Override
    public CircuitBreakerConfig getConfig(String group, String tag) {
        return rules.containsKey(group) ? rules.get(group).get(tag) : null;
    }

    @Override
    public void addConfig(CircuitBreakerConfig config) {
        ICircuitBreaker circuitBreaker = ExtensionLoader.getLoader(ICircuitBreaker.class).getExtension(config.getMode().getValue());
        circuitBreaker.addListener(this);

        circuitBreakers.put(config.identity(), circuitBreaker);
        rules.computeIfAbsent(config.getGroup(), k -> new ConcurrentHashMap<>()).put(config.getTag(), config);
    }

    @Override
    public void checkAndAddConfig(CircuitBreakerConfig config) {
        if (!circuitBreakers.containsKey(config.identity())) {
            addConfig(config);
        }
    }

    @Override
    public void onEvent(CircuitBreakerConfig config, EventType eventType, Object... args) {
        log.info("Receive circuit-breaker[{}] event[{}]", config.identity(), eventType);
    }

    @Override
    public void notify(CircuitBreakerConfig config) throws Exception {
        ICircuitBreaker circuitBreaker = circuitBreakers.get(config.identity());
        if (null == circuitBreaker) {
            log.warn("Notfound circuit-breaker[{}]", config.identity());
            return;
        }
        if (circuitBreaker.refresh(config)) {
            log.info("Circuit-breaker[{}] config refresh success: {}", config.identity(), config);
            return;
        }

        log.warn("Circuit-breaker[{}] config refresh failure: {}", config.identity(), config);
    }

    @Override
    public Object originalCall(String identity, OriginalCall originalCall, OriginalContext originalContext) throws Throwable {
        try {
            OriginalContext.set(originalContext);
            if (null == identity || !circuitBreakers.containsKey(identity)) {
                return originalCall.call(originalContext);
            }

            ICircuitBreaker circuitBreaker = circuitBreakers.get(identity);
            return circuitBreaker.wrapperCall(originalContext, originalCall);
        } finally {
            OriginalContext.remove();
        }
    }

    @Override
    public Map<String, Map<String, Long>> collect() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        for (Map.Entry<String, ICircuitBreaker> entry : circuitBreakers.entrySet()) {
            Map<String, Long> tempMap = entry.getValue().collect();
            if (!tempMap.isEmpty()) {
                dataMap.put(entry.getKey(), tempMap);
            }
        }

        return dataMap;
    }

}
