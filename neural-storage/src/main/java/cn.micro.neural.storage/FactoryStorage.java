package cn.micro.neural.storage;

import lombok.Getter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.Serializable;
import java.util.List;

/**
 * FactoryStorage
 *
 * @author lry
 */
@Getter
public enum FactoryStorage {

    // ===

    INSTANCE;

    private IStorage storage;

    public void setRedisTemplate(RedisTemplate<String, Serializable> redisTemplate) {
        this.storage = new IStorage() {
            @Override
            public Number[] eval(String script, List<String> keys, Object... args) {
                return redisTemplate.execute(new DefaultRedisScript<>(script, Number[].class), keys, args);
            }

            @Override
            public boolean set(String key, Object value) {
                return false;
            }

            @Override
            public boolean setEx(String key, Object value, Long expireTime) {
                return false;
            }

            @Override
            public boolean exists(String key) {
                return false;
            }

            @Override
            public Object get(String key) {
                return null;
            }

            @Override
            public boolean remove(String key) {
                return false;
            }
        };
    }

}
