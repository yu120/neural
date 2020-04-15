package cn.micro.neural.limiter.support;

import cn.micro.neural.limiter.ILimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

/**
 * RedisLuaLimiter
 *
 * @author lry
 */
@Slf4j
public class RedisLuaLimiter implements ILimiter {

    @Override
    public boolean limit(String key, long limitCount, long limitPeriod) {
        List<String> keys = Collections.singletonList(StringUtils.join(limiter.prefix(), key));

        try {
            String luaScript = buildLuaScript();
            RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
            Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
            log.info("Access try count is {} for name={} and key = {}", count, name, key);
            return count != null && count.longValue() <= limitCount;
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw new RuntimeException(e.getLocalizedMessage());
            }
            throw new RuntimeException("server exception");
        }

        return false;
    }

    /**
     * 编写 redis Lua 限流脚本
     * <p>
     * KEYS[1]：prefix+key
     * ARGV[1]：limitCount
     * ARGV[2]：limitPeriod
     *
     * @return lua script
     */
    private String buildLuaScript() {
        return "local c = redis.call('get',KEYS[1])" +
                // 调用不超过最大值，则直接返回
                "\nif c and tonumber(c) > tonumber(ARGV[1]) then" +
                "\nreturn c" +
                "\nend" +
                // 执行计算器自加
                "\nc = redis.call('incr',KEYS[1])" +
                "\nif tonumber(c) == 1 then" +
                // 从第一次调用开始限流，设置对应键值的过期
                "\nredis.call('expire', KEYS[1], ARGV[2])" +
                "\nend" +
                "\nreturn c";
    }

}
