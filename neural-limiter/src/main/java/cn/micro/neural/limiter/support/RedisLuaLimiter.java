package cn.micro.neural.limiter.support;

import cn.micro.neural.limiter.ILimiter;
import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.storage.FactoryStorage;
import cn.neural.common.extension.Extension;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RedisTemplateLimiter
 *
 * @author lry
 */
@Slf4j
@Extension("redis-lua")
public class RedisLuaLimiter implements ILimiter {

    public static final String RATE_LIMITER = "/rate_limiter.lua";

    private String script;
    private LimiterConfig limiterConfig;

    @Override
    public void initialize(LimiterConfig limiterConfig) throws Exception {
        this.script = CharStreams.toString(new InputStreamReader(
                this.getClass().getResource(RATE_LIMITER).openStream(), StandardCharsets.UTF_8));
        this.limiterConfig = limiterConfig;
    }

    @Override
    public boolean callRate(String key, long maxLimit, long limitPeriod) {
        String wrapperKey = limiterConfig.getPrefix() + key;
        List<String> keys = Collections.singletonList(wrapperKey);
        Number[] result = FactoryStorage.INSTANCE.getStorage().eval(script, keys, maxLimit, limitPeriod);
        log.debug("Call[{}] rate count[{}]", wrapperKey, result == null ? null : Arrays.asList(result));
        return result == null || result.length != 2 || result[0].longValue() == 1;
    }

    @Override
    public void destroy() {

    }

}
