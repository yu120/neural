package cn.micro.neural.limiter.storage;

import cn.micro.neural.limiter.LimiterConfig;
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
    private LimiterConfig limiterConfig;

    public void initialize(LimiterConfig limiterConfig) throws Exception {
        this.limiterConfig = limiterConfig;
    }

    public void setRedisTemplate(RedisTemplate<String, Serializable> redisTemplate) {
        this.storage = new IStorage() {
            @Override
            public Number[] eval(String script, List<String> keys, Object... args) {
                return redisTemplate.execute(new DefaultRedisScript<>(script, Number[].class), keys, args);
            }
        };
    }
}
