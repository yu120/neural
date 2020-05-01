package cn.micro.neural.idempotent;

import cn.micro.neural.idempotent.core.Idempotent;
import cn.micro.neural.idempotent.event.EventListener;
import cn.micro.neural.idempotent.event.EventType;
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
 * IdempotentFactory
 *
 * @author lry
 **/
@Slf4j
@Getter
@Extension(IdempotentFactory.IDENTITY)
public class IdempotentFactory implements EventListener, Neural<IdempotentConfig> {

    public static final String IDENTITY = "idempotent";

    /**
     * Map<key=IdempotentConfig#identity(), Idempotent>
     */
    private final ConcurrentMap<String, Idempotent> idempotentMap = new ConcurrentHashMap<>();
    /**
     * Map<key=IdempotentConfig#getGroup(), subKey=IdempotentConfig#getTag(), value=IdempotentConfig>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, IdempotentConfig>> rules = new ConcurrentHashMap<>();

    @Override
    public IdempotentConfig getConfig(String group, String tag) {
        return rules.containsKey(group) ? rules.get(group).get(tag) : null;
    }

    @Override
    public void addConfig(IdempotentConfig config) {
        Idempotent idempotent = ExtensionLoader.getLoader(Idempotent.class).getExtension(config.getMode().getValue());
        idempotent.addListener(this);

        idempotentMap.put(config.identity(), idempotent);
        rules.computeIfAbsent(config.getGroup(), k -> new ConcurrentHashMap<>()).put(config.getTag(), config);
    }

    @Override
    public void checkAndAddConfig(IdempotentConfig config) {
        if (!idempotentMap.containsKey(config.identity())) {
            addConfig(config);
        }
    }

    @Override
    public void onEvent(IdempotentConfig config, EventType eventType, Object... args) {
        log.info("Receive idempotent[{}] event[{}]", config.identity(), eventType);
    }

    @Override
    public void notify(IdempotentConfig config) throws Exception {
        Idempotent idempotent = idempotentMap.get(config.identity());
        if (null == idempotent) {
            log.warn("Notfound idempotent[{}]", config.identity());
            return;
        }
        if (idempotent.refresh(config)) {
            log.info("Idempotent[{}] config refresh success: {}", config.identity(), config);
            return;
        }

        log.warn("Idempotent[{}] config refresh failure: {}", config.identity(), config);
    }

    @Override
    public Object originalCall(String identity, OriginalCall originalCall, final OriginalContext originalContext) throws Throwable {
        try {
            OriginalContext.set(originalContext);
            if (null == identity || !idempotentMap.containsKey(identity)) {
                return originalCall.call(originalContext);
            }

            Idempotent idempotent = idempotentMap.get(identity);
            return idempotent.wrapperCall(originalContext, originalCall);
        } finally {
            OriginalContext.remove();
        }
    }


    @Override
    public Map<String, Map<String, Long>> collect() {
        final Map<String, Map<String, Long>> dataMap = new LinkedHashMap<>();
        for (Map.Entry<String, Idempotent> entry : idempotentMap.entrySet()) {
            Map<String, Long> tempMap = entry.getValue().collect();
            if (!tempMap.isEmpty()) {
                dataMap.put(entry.getKey(), tempMap);
            }
        }

        return dataMap;
    }

}
