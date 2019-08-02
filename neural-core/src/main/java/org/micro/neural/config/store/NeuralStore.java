package org.micro.neural.config.store;

import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.redisson.Redisson;
import org.redisson.api.*;
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
public enum NeuralStore {

    //===

    INSTANCE;

    private boolean started;
    private RedissonClient redissonClient;
    private Map<String, Integer> channelListenerIds = new ConcurrentHashMap<>();

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

    public void batchAdd(String space, Map<String, String> data) {
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
    public Map<String, String> query(String name) {
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

    public void publish(String channel, Object data) {
        RTopic topic = redissonClient.getTopic(channel, new SerializationCodec());
        topic.publish(SerializeUtils.serialize(data));
    }

    public void subscribe(Collection<String> channels, IStoreListener listener) {
        RTopic topic = redissonClient.getTopic("dw", new SerializationCodec());
        topic.addListener(String.class, (charSequence, message) ->
                listener.notify(charSequence.toString(), message));
    }

    /**
     * The destroy
     */
    public void destroy() {
        if (null != redissonClient) {
            redissonClient.shutdown();
        }
    }

}
