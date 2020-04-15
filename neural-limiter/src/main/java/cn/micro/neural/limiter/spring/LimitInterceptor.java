package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.ILimiter;
import cn.micro.neural.limiter.Limiter;
import cn.micro.neural.limiter.LimiterException;
import cn.micro.neural.limiter.support.RedisTemplateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;
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

    @Bean
    public RedisTemplate<String, Serializable> limitRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Around("execution(public * *(..)) && @annotation(cn.micro.neural.limiter.Limiter)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Limiter limiter = method.getAnnotation(Limiter.class);

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String key;
        switch (limiter.type()) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOMER:
                key = limiter.key();
                break;
            default:
                key = method.getName().toUpperCase();
        }

        boolean result;
        try {
            ILimiter limit = new RedisTemplateLimiter();
            result = limit.callRate(key, limiter.count(), limiter.period());
        } catch (Throwable e) {
            throw new RuntimeException("server exception");
        }

        if (result) {
            return pjp.proceed();
        }
        throw new LimiterException("You have been dragged into the blacklist");
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