package cn.micro.neural.limiter;

import cn.micro.neural.limiter.core.ILimiter;
import cn.micro.neural.limiter.event.EventListener;
import cn.micro.neural.limiter.event.EventType;
import cn.micro.neural.storage.Neural;
import cn.neural.common.extension.Extension;
import cn.neural.common.extension.ExtensionLoader;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LimiterFactory
 *
 * @author lry
 **/
@Slf4j
@Getter
@Extension(LimiterFactory.IDENTITY)
public class LimiterFactory implements EventListener, Neural<LimiterConfig> {

    public static final String IDENTITY = "limiter";

    /**
     * Map<key=IdempotentConfig#identity(), ILimiter>
     */
    private final ConcurrentMap<String, ILimiter> limiters = new ConcurrentHashMap<>();
    /**
     * Map<key=LimiterConfig#getGroup(), subKey=LimiterConfig#getTag(), value=LimiterConfig>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, LimiterConfig>> rules = new ConcurrentHashMap<>();

    @Override
    public LimiterConfig getConfig(String group, String tag) {
        return rules.containsKey(group) ? rules.get(group).get(tag) : null;
    }

    @Override
    public void addConfig(LimiterConfig config) {
        ILimiter limiter = ExtensionLoader.getLoader(ILimiter.class).getExtension(config.getMode().getValue());
        limiter.addListener(this);

        limiters.put(config.identity(), limiter);
        rules.computeIfAbsent(config.getGroup(), k -> new ConcurrentHashMap<>()).put(config.getTag(), config);
    }

    @Override
    public void checkAndAddConfig(LimiterConfig config) {
        if (!limiters.containsKey(config.identity())) {
            addConfig(config);
        }
    }

    @Override
    public void onEvent(LimiterConfig config, EventType eventType, Object... args) {
        log.info("Receive limiter[{}] event[{}]", config.identity(), eventType);
    }

    @Override
    public void notify(LimiterConfig config) throws Exception {
        ILimiter limiter = limiters.get(config.identity());
        if (null == limiter) {
            log.warn("Notfound limiter[{}]", config.identity());
            return;
        }
        if (limiter.refresh(config)) {
            log.info("Limiter[{}] config refresh success: {}", config.identity(), config);
            return;
        }

        log.warn("Limiter[{}] config refresh failure: {}", config.identity(), config);
    }

    @Override
    public Object originalCall(String identity, OriginalCall originalCall, final OriginalContext originalContext) throws Throwable {
        try {
            OriginalContext.set(originalContext);
            if (null == identity || !limiters.containsKey(identity)) {
                return originalCall.call(originalContext);
            }

            ILimiter limiter = limiters.get(identity);
            return limiter.wrapperCall(originalContext, originalCall);
        } finally {
            OriginalContext.remove();
        }
    }

    @Override
    public Map<String, Map<String, Long>> collect() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        for (Map.Entry<String, ILimiter> entry : limiters.entrySet()) {
            Map<String, Long> tempMap = entry.getValue().collect();
            if (!tempMap.isEmpty()) {
                dataMap.put(entry.getKey(), tempMap);
            }
        }

        return dataMap;
    }

}
