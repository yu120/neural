package org.micro.neural.config.store;

import io.lettuce.core.api.StatefulRedisConnection;
import org.micro.neural.common.URL;
import org.micro.neural.extension.SPI;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Store With Config.
 *
 * @author lry
 **/
@SPI(single = true)
public interface IStore {

    /**
     * The initialize store
     *
     * @param url {@link URL}
     */
    void initialize(URL url);

    /**
     * The get generic object
     *
     * @return generic object
     */
    Object object();

    /**
     * The add Map with key
     *
     * @param space space key
     * @param key   storeK key
     * @param data  config data
     */
    void add(String space, String key, Object data);

    /**
     * The batch add
     *
     * @param space space
     * @param data  config data
     */
    void batchAdd(String space, Map<String, String> data);

    /**
     * The batch increment update or add
     *
     * @param key    key
     * @param data   config data
     * @param expire expire time
     */
    void batchIncrementBy(String key, Map<String, Object> data, long expire);

    /**
     * The search keys with keyword
     *
     * @param space   space key
     * @param keyword config key keyword
     * @return config key list
     */
    Set<String> search(String space, String keyword);

    /**
     * The query Map with key
     *
     * @param space space key
     * @param key   store key
     * @param clz   config object class
     * @return config object
     */
    <C> C query(String space, String key, Class<C> clz);

    /**
     * The get key
     *
     * @param key key
     * @return value
     */
    String get(String key);

    /**
     * The lua script eval
     *
     * @param script  script
     * @param timeout timeout(ms): borrowMaxWaitMillis = 0.8 * timeout, future.get(timeout)
     * @param keys    key list
     * @return object by <T>
     */
    List<Object> eval(String script, Long timeout, List<Object> keys);

    /**
     * The pull config
     *
     * @param key hash type key
     * @return hash key-value
     */
    Map<String, String> pull(String key);

    /**
     * The publish
     *
     * @param channel channel key
     * @param data    config data
     */
    void publish(String channel, Object data);

    /**
     * The subscribe by subscribeKey
     *
     * @param channels channel list
     * @param listener {@link IStoreListener}
     */
    void subscribe(Collection<String> channels, IStoreListener listener);

    /**
     * The un subscribe by SubscribeListener
     *
     * @param listener {@link IStoreListener}
     */
    void unsubscribe(IStoreListener listener);

    /**
     * The destroy
     */
    void destroy();

    StatefulRedisConnection<String, String> borrowObject();

    StatefulRedisConnection<String, String> borrowObject(long borrowMaxWaitMillis);

    void returnObject(StatefulRedisConnection<String, String> connection);

}
