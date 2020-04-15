package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.Limiter;
import cn.micro.neural.limiter.LimitType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 限流切面实现
 *
 * @author lry
 */
@Slf4j
@Aspect
@Configuration
public class LimitInterceptor {

    @Autowired
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    @Bean
    public RedisTemplate<String, Serializable> limitRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Around("execution(public * *(..)) && @annotation(cn.micro.neural.limiter.Limiter)")
    public Object interceptor(ProceedingJoinPoint pjp) {
        Method method =  ((MethodSignature) pjp.getSignature()).getMethod();

        Limiter limiter = method.getAnnotation(Limiter.class);
        LimitType type = limiter.type();
        String name = limiter.name();
        int limitPeriod = limiter.period();
        int limitCount = limiter.count();

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String key;
        switch (type) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOMER:
                key = limiter.key();
                break;
            default:
                key = StringUtils.upperCase(method.getName());
        }

        List<String> keys = Collections.singletonList(StringUtils.join(limiter.prefix(), key));

        try {
            String luaScript = buildLuaScript();
            RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
            Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
            log.info("Access try count is {} for name={} and key = {}", count, name, key);
            if (count != null && count.intValue() <= limitCount) {
                return pjp.proceed();
            } else {
                throw new RuntimeException("You have been dragged into the blacklist");
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw new RuntimeException(e.getLocalizedMessage());
            }
            throw new RuntimeException("server exception");
        }
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

    /**
     * 获取id地址
     *
     * @return Ip address
     */
    private String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
                RequestContextHolder.getRequestAttributes())).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

}