package org.micro.neural.config.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.micro.neural.common.URL;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.extension.Extension;

/**
 * The Store by Redis
 * <p>
 * TODO:死信问题怎么处理？
 *
 * @author lry
 **/
@Slf4j
@Extension("redis")
public class RedisStore implements IStore {

    private RedisClient redisClient = null;
    private GenericObjectPool<StatefulRedisConnection<String, String>> objectPool;
    private final Map<IStoreListener, RedisPubSubAsyncCommands<String, String>> subscribed = new ConcurrentHashMap<>();

    @Override
    public void initialize(URL url) {
        RedisURI redisURI;
        String category = url.getParameter(URL.CATEGORY_KEY, "redis");
        if ("sentinel".equals(category)) {
            redisURI = RedisURI.Builder.sentinel(url.getHost(), url.getPort()).build();
        } else {
            redisURI = RedisURI.Builder.redis(url.getHost(), url.getPort()).build();
        }

        String password = url.getParameter("password");
        if (password != null && password.length() > 0) {
            redisURI.setPassword(password);
        }

        this.redisClient = RedisClient.create(redisURI);
        this.objectPool = ConnectionPoolSupport.createGenericObjectPool(
                () -> redisClient.connect(), new GenericObjectPoolConfig());
    }

    @Override
    public void batchUpOrAdd(long expire, Map<String, Long> data) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisAsyncCommands<String, String> commands = connection.async();
                for (Map.Entry<String, Long> entry : data.entrySet()) {
                    commands.incrby(entry.getKey(), entry.getValue());
                    commands.pexpire(entry.getKey(), expire);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void add(String space, String key, Object data) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                commands.hset(space, key, SerializeUtils.serialize(data));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Set<String> searchKeys(String space, String keyword) {
        Set<String> keySet = new HashSet<>();
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                List<String> keys = commands.hkeys(space);
                if (keys == null || keys.isEmpty()) {
                    return keySet;
                }

                keySet.addAll(keys);
                return keySet;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <C> C query(String space, String key, Class<C> clz) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                return SerializeUtils.deserialize(clz, commands.hget(space, key));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                return commands.get(key);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T eval(Class<T> type, String script, Long timeout, String[] keys, String[] values) {
        ScriptOutputType scriptOutputType;
        String typeName = type.getName();
        if (Boolean.class.getName().equals(typeName)) {
            scriptOutputType = ScriptOutputType.BOOLEAN;
        } else if (Integer.class.getName().equals(typeName)) {
            scriptOutputType = ScriptOutputType.INTEGER;
        } else {
            scriptOutputType = ScriptOutputType.MULTI;
        }

        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisAsyncCommands<String, String> commands = connection.async();
                RedisFuture<T> redisFuture = commands.eval(script, scriptOutputType, keys, values);
                T result = redisFuture.get(timeout, TimeUnit.MILLISECONDS);
                if (String.class.getName().equals(typeName)) {
                    return result;
                }

                return SerializeUtils.deserialize(type, (String) result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> pull(String key) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                return commands.hgetall(key);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void publish(String channel, Object data) {
        try {
            try (StatefulRedisConnection<String, String> connection = objectPool.borrowObject()) {
                RedisCommands<String, String> commands = connection.sync();
                commands.publish(channel, SerializeUtils.serialize(data));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribe(Collection<String> channels, IStoreListener listener) {
        StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();
        connection.addListener(new RedisPubSubListener<String, String>() {

            @Override
            public void message(String channel, String message) {
                log.debug("message={} on channel {}", message, channel);
                listener.notify(channel, message);
            }

            @Override
            public void subscribed(String channel, long count) {
                log.debug("subscribed channel={}, count={}", channel, count);
            }

            @Override
            public void unsubscribed(String channel, long count) {
                log.debug("unsubscribed channel={}, count={}", channel, count);
            }

            @Override
            public void message(String pattern, String channel, String message) {
                log.debug("pattern message={} in channel={}", message, channel);
            }

            @Override
            public void psubscribed(String pattern, long count) {
                log.debug("pattern subscribed pattern={}, count={}", pattern, count);
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                log.debug("pattern unsubscribed channel={}, count={}", pattern, count);
            }

        });
        RedisPubSubAsyncCommands<String, String> redisPubSubAsyncCommands = connection.async();
        subscribed.put(listener, redisPubSubAsyncCommands);
        redisPubSubAsyncCommands.subscribe(channels.toArray(new String[0]));
    }

    @Override
    public void unSubscribe(IStoreListener listener) {
        RedisPubSubAsyncCommands<String, String> commands = subscribed.get(listener);
        if (commands != null) {
            commands.unsubscribe();
            subscribed.remove(listener);
        }
    }

    @Override
    public void destroy() {
        if (null != objectPool) {
            objectPool.close();
        }
        if (null != redisClient) {
            redisClient.shutdown();
        }
    }

}
