package cn.micro.neural.limiter.support;

import cn.micro.neural.limiter.ILimiter;
import cn.micro.neural.limiter.LimiterConfig;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * RedisLuaLimiter
 *
 * @author lry
 */
@Slf4j
public class RedisLuaLimiter implements ILimiter {

    public static final String FILE_NAME = "/request_rate_limiter.lua";

    private String script;
    private LimiterConfig limiterConfig;

    @Autowired
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    @Override
    public void initialize(LimiterConfig limiterConfig) throws Exception {
        this.script = CharStreams.toString(new InputStreamReader(
                this.getClass().getResource(FILE_NAME).openStream(), StandardCharsets.UTF_8));
        this.limiterConfig = limiterConfig;
    }

    @Override
    public boolean callRate(String key, long maxLimit, long limitPeriod) {
        String wrapperKey = limiterConfig.getPrefix() + key;
        RedisScript<Number[]> redisScript = new DefaultRedisScript<>(script, Number[].class);
        List<String> keys = Collections.singletonList(wrapperKey);
        Number[] count = limitRedisTemplate.execute(redisScript, keys, maxLimit, limitPeriod);
        log.info("Access try count is {} for name={} and key = {}", count, "", key);
        return count == null || count.length != 2 || count[0].longValue() == 1;
    }

    @Override
    public void destroy() {

    }

}
