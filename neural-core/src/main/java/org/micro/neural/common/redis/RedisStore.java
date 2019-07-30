package org.micro.neural.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.IStoreListener;
import org.micro.neural.extension.Extension;
import org.redisson.api.*;
import org.redisson.codec.SerializationCodec;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The Store by Redis
 * <p>
 *
 * @author lry
 **/
@Slf4j
@Extension("redis")
public class RedisStore implements IStore {

    private RedissonClient redissonClient;

    @Override
    public void initialize(URL url) {
        RedisFactory redisFactory = RedisFactory.INSTANCE;
        redisFactory.initialize(url);
        this.redissonClient = redisFactory.getRedissonClient();
    }

    @Override
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

    @Override
    public void add(String space, String key, Object data) {
        redissonClient.getMap(space).put(key, SerializeUtils.serialize(data));
    }

    @Override
    public void batchAdd(String space, Map<String, String> data) {
        redissonClient.getMap(space).putAll(data);
    }

    @Override
    public List<Object> eval(String script, Long timeout, List<Object> keys) {
        List<Object> keyArray = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            Object obj = keys.get(i);
            if (obj == null) {
                throw new IllegalArgumentException("The key[" + i + "] is null");
            }

            keyArray.add(obj);
        }

        RFuture<List<Object>> redisFuture = redissonClient.getScript().evalAsync(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.MAPVALUELIST, keyArray);
        try {
            return redisFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> pull(String key) {
        Map<Object, Object> remoteMap = redissonClient.getMap(key);
        if (remoteMap == null || remoteMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : remoteMap.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return map;
    }

    @Override
    public void publish(String channel, Object data) {
        RTopic topic = redissonClient.getTopic(channel, new SerializationCodec());
        topic.publish(SerializeUtils.serialize(data));
    }

    @Override
    public void subscribe(Collection<String> channels, IStoreListener listener) {
        RTopic topic = redissonClient.getTopic("dw", new SerializationCodec());
        topic.addListener(String.class, (charSequence, message) ->
                listener.notify(charSequence.toString(), message));
    }

}
