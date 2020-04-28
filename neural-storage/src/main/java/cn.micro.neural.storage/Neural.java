package cn.micro.neural.storage;

import java.util.Map;

public interface Neural<C> {

    /**
     * The add config
     *
     * @param group group key
     * @param tag   tag key
     * @return {@link C}
     */
    C getConfig(String group, String tag);

    /**
     * The add config
     *
     * @param config {@link C}
     */
    void addConfig(C config);

    /**
     * The check and add config
     *
     * @param config {@link C}
     */
    void checkAndAddConfig(C config);

    /**
     * The notify of changed config
     *
     * @param config {@link C}
     * @throws Exception exception
     */
    void notify(C config) throws Exception;

    /**
     * The process of original call
     *
     * @param identity     config identity
     * @param originalCall {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    Object originalCall(String identity, OriginalCall originalCall) throws Throwable;

    /**
     * The process of original call
     *
     * @param identity        config identity
     * @param originalCall    {@link OriginalCall}
     * @param originalContext {@link OriginalContext}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    Object originalCall(String identity, OriginalCall originalCall, final OriginalContext originalContext) throws Throwable;

    /**
     * The collect of get and reset statistics data
     *
     * @return statistics data
     */
    Map<String, Map<String, Long>> collect();

    /**
     * The get statistics data
     *
     * @return statistics data
     */
    Map<String, Map<String, Long>> statistics();

}
