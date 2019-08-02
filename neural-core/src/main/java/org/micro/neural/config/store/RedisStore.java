package org.micro.neural.config.store;

import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.listener.PatternMessageListener;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Neural Store
 *
 * @author lry
 */
@Slf4j
public enum RedisStore {

    //===

    INSTANCE;

    private boolean started;
    private RedissonClient redissonClient;
    private Map<String, PatternMessageListener> patternListeners = new ConcurrentHashMap<>();

    /**
     * The initialize store
     *
     * @param url {@link URL}
     */
    public synchronized void initialize(URL url) {
        if (started) {
            return;
        }

        Config config = new Config();

        String category = url.getParameter(URL.CATEGORY_KEY);
        RedisModel redisModel = RedisModel.parse(category);
        if (RedisModel.SENTINEL == redisModel) {
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
            sentinelServersConfig.addSentinelAddress(url.getAddresses());
        } else if (RedisModel.CLUSTER == redisModel) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            clusterServersConfig.addNodeAddress(url.getAddresses());
        } else if (RedisModel.MASTER_SLAVE == redisModel) {
            MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers();
            masterSlaveServersConfig.setMasterAddress(url.getAddress());
            masterSlaveServersConfig.setSlaveAddresses(new HashSet<>(url.getBackupAddressList()));
        } else if (RedisModel.REPLICATED == redisModel) {
            ReplicatedServersConfig replicatedServersConfig = config.useReplicatedServers();
            replicatedServersConfig.addNodeAddress(url.getAddresses());
        } else {
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(url.getAddress());
        }

        this.redissonClient = Redisson.create(config);
        this.started = true;
    }

    public void batchIncrementBy(String key, Map<String, Object> data, long expire) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Long) {
                redissonClient.getMap(key).addAndGet(entry.getKey(), (Long) entry.getValue());
            } else {
                redissonClient.getMap(key).put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        redissonClient.getMap(key).expire(expire, TimeUnit.MILLISECONDS);
    }

    /**
     * The put all map
     *
     * @param space space
     * @param data  map data
     */
    public void putAllMap(String space, Map<String, String> data) {
        redissonClient.getMap(space).putAll(data);
    }

    /**
     * The execute lua script
     *
     * @param script  lua script
     * @param timeout future timeout
     * @param keys    key list
     * @return return object list
     */
    public List<Object> eval(String script, Long timeout, List<Object> keys) {
        List<Object> keyArray = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            Object obj = keys.get(i);
            if (obj == null) {
                throw new IllegalArgumentException("The key[" + i + "] is null");
            }

            keyArray.add(obj);
        }

        try {
            RFuture<List<Object>> redisFuture = redissonClient.getScript().evalAsync(
                    RScript.Mode.READ_WRITE, script, RScript.ReturnType.MULTI, keyArray);
            return redisFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * The get all key-value by name
     *
     * @param name map name
     * @return map
     */
    public Map<String, String> getMap(String name) {
        Map<Object, Object> remoteMap = redissonClient.getMap(name);
        if (remoteMap == null || remoteMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : remoteMap.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return map;
    }

    /**
     * The publish
     *
     * @param channel channel
     * @param data    data
     */
    public void publish(String channel, String data) {
        RTopic topic = redissonClient.getTopic(channel, new SerializationCodec());
        topic.publish(SerializeUtils.serialize(data));
    }

    /**
     * The subscribe by pattern
     *
     * @param pattern  pattern
     * @param listener {@link IStoreListener}
     */
    public void subscribe(String pattern, IStoreListener listener) {
        if (patternListeners.containsKey(pattern)) {
            log.warn("The repeated subscribe:{}, listener:{}", pattern, listener);
            return;
        }

        PatternMessageListener<String> pmListener = (pattern1, channel, msg) -> {
            log.debug("The notify message pattern:{}, channel:{}, msg: {}", pattern1, channel, msg);
            listener.notify(channel.toString(), msg);
        };
        patternListeners.put(pattern, pmListener);

        RPatternTopic rPatternTopic = redissonClient.getPatternTopic(pattern);
        rPatternTopic.addListener(String.class, pmListener);
    }

    /**
     * The pattern unsubscribe
     *
     * @param pattern pattern
     */
    public void unsubscribe(String pattern) {
        PatternMessageListener patternMessageListener = patternListeners.get(pattern);
        if (patternMessageListener == null) {
            return;
        }

        RPatternTopic rPatternTopic = redissonClient.getPatternTopic(pattern);
        rPatternTopic.removeListener(patternMessageListener);
    }

    /**
     * The destroy
     */
    public void destroy() {
        for (Map.Entry<String, PatternMessageListener> entry : patternListeners.entrySet()) {
            unsubscribe(entry.getKey());
        }
        if (null != redissonClient) {
            redissonClient.shutdown();
        }
    }

}
