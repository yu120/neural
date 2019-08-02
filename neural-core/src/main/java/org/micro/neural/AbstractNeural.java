package org.micro.neural;

import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.GlobalConfig.*;
import org.micro.neural.config.RuleConfig;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;
import org.micro.neural.extension.SPI;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * The Abstract Neural
 *
 * @param <C> extends {@link RuleConfig}
 * @param <G> extends {@link GlobalConfig}
 * @author lry
 **/
@SPI
@Slf4j
public abstract class AbstractNeural<C extends RuleConfig, G extends GlobalConfig> implements Neural<C, G> {

    private Class<C> ruleClass;
    private Class<G> globalClass;

    private Extension extension;
    private StorePool storePool;

    protected volatile G globalConfig;
    protected volatile ConcurrentMap<String, C> configs = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public AbstractNeural() {
        Type type = this.getClass().getGenericSuperclass();
        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
        this.ruleClass = (Class<C>) args[0];
        this.globalClass = (Class<G>) args[1];
        this.extension = this.getClass().getAnnotation(Extension.class);
        this.storePool = StorePool.INSTANCE;
        storePool.register(extension.value().toUpperCase(), this);
    }

    @Override
    public void initialize(URL url) {
        storePool.initialize(url);
    }

    @Override
    public G getGlobalConfig() {
        if (globalConfig == null) {
            try {
                this.globalConfig = globalClass.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        return globalConfig;
    }

    @Override
    public void addConfig(C config) {
        if (config.getApplication() == null || config.getApplication().length() == 0 ||
                config.getGroup() == null || config.getGroup().length() == 0 ||
                config.getResource() == null || config.getResource().length() == 0) {
            throw new IllegalArgumentException("application, group, resource cannot be empty at the same time");
        }
        config.setModule(extension.value());
        storePool.register(extension.value().toUpperCase(), config.identity(), SerializeUtils.serialize(config));
        configs.put(config.identity(), config);
    }

    @Override
    public Object originalCall(String identity, OriginalCall originalCall) throws Throwable {
        return originalCall(new NeuralContext(), identity, originalCall);
    }

    @Override
    public Object originalCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        try {
            NeuralContext.set(neuralContext);
            return wrapperCall(neuralContext, identity, originalCall);
        } finally {
            NeuralContext.remove();
        }
    }

    public Object wrapperCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        return null;
    }

    @Override
    public void notify(Category category, String identity, String data) {
        if (Category.GLOBAL == category) {
            this.globalConfig = SerializeUtils.deserialize(globalClass, data);
            globalNotify(globalConfig);
        } else if (Category.RULE == category) {
            C ruleConfig = SerializeUtils.deserialize(ruleClass, data);
            configs.put(identity, ruleConfig);
            ruleNotify(identity, ruleConfig);
        }
    }

    @Override
    public Map<String, Map<String, Long>> collect() {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<String, Map<String, Long>> statistics() {
        return new LinkedHashMap<>();
    }

    /**
     * The notify of global config
     *
     * @param globalConfig {@link G}
     */
    protected void globalNotify(G globalConfig) {
        log.debug("The notify of global config: {}", globalConfig);
    }

    /**
     * The notify of rule config
     *
     * @param identity   identity
     * @param ruleConfig {@link C}
     */
    protected void ruleNotify(String identity, C ruleConfig) {
        log.debug("The rule of global config: {}", globalConfig);
    }

    @Override
    public void destroy() {

    }

}
