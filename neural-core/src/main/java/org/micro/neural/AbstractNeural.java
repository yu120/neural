package org.micro.neural;

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
 * The Store Config
 *
 * @param <C> extends {@link RuleConfig}
 * @param <G> extends {@link GlobalConfig}
 * @author lry
 **/
@SPI
@Slf4j
public abstract class AbstractNeural<C extends RuleConfig, G extends GlobalConfig> implements Neural<C, G> {

    private final Class<C> ruleClass;
    private final Class<G> globalClass;

    protected volatile G globalConfig;
    protected volatile ConcurrentMap<String, C> configs = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public AbstractNeural() {
        Type type = this.getClass().getGenericSuperclass();
        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
        this.ruleClass = (Class<C>) args[0];
        this.globalClass = (Class<G>) args[1];

        try {
            this.globalConfig = globalClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        Extension extension = this.getClass().getAnnotation(Extension.class);
        StorePool.getInstance().register(extension.value(), this, SerializeUtils.serialize(globalConfig));
    }

    @Override
    public G getGlobalConfig() {
        return globalConfig;
    }

    @Override
    public void addConfig(C config) {
        if (config.getApplication() == null || config.getApplication().length() == 0 ||
                config.getGroup() == null || config.getGroup().length() == 0 ||
                config.getResource() == null || config.getResource().length() == 0) {
            throw new IllegalArgumentException("application, group, resource cannot be empty at the same time");
        }
        configs.put(config.identity(), config);
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
    public Map<String, Long> collect() {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<String, Long> statistics() {
        return new LinkedHashMap<>();
    }

    @Override
    public void destroy() {

    }

    /**
     * The execute notify
     *
     * @param globalConfig {@link G}
     */
    protected void globalNotify(G globalConfig) {

    }

    /**
     * The execute notify
     *
     * @param identity   identity
     * @param ruleConfig {@link C}
     */
    protected abstract void ruleNotify(String identity, C ruleConfig);

}
