package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.Limiter;
import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterContext;
import cn.neural.common.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * LimitInterceptor
 *
 * @author lry
 */
@Slf4j
@Aspect
@Configuration
@EnableConfigurationProperties(LimiterRuleConfig.class)
public class LimiterInterceptor implements InitializingBean {

    private final Limiter limiter = new Limiter();

    @Autowired
    private LimiterRuleConfig limiterRuleConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        Set<Class<?>> classSet = ClassUtils.getClasses("");
        for (Class<?> clazz : classSet) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                NeuralLimiter neuralLimiter = method.getAnnotation(NeuralLimiter.class);
                if (neuralLimiter == null) {
                    continue;
                }

                LimiterConfig limiterConfig = new LimiterConfig();
                // description configuration
                limiterConfig.setTag(neuralLimiter.value());
                limiterConfig.setGroup(neuralLimiter.group());
                limiterConfig.setName(neuralLimiter.name());
                if (neuralLimiter.labels().length > 0) {
                    limiterConfig.setLabels(Arrays.asList(neuralLimiter.labels()));
                }
                limiterConfig.setIntro(neuralLimiter.intro());
                
                // rule configuration
                limiterConfig.setNode(limiterRuleConfig.getNode());
                limiterConfig.setApplication(limiterRuleConfig.getApplication());
                limiterConfig.setEnable(limiterRuleConfig.getEnable());
                limiterConfig.setMode(limiterRuleConfig.getMode());
                limiterConfig.setStrategy(limiterRuleConfig.getStrategy());
                limiterConfig.setRate(limiterRuleConfig.getRate());
                limiterConfig.setRequest(limiterRuleConfig.getRequest());
                limiterConfig.setConcurrent(limiterRuleConfig.getConcurrent());
                limiter.addLimiter(limiterConfig);
            }
        }
    }

    @Around("execution(public * *(..)) && @annotation(cn.micro.neural.limiter.spring.NeuralLimiter)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        NeuralLimiter neuralLimiter = method.getAnnotation(NeuralLimiter.class);

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String key;
        if (LimitType.IP == neuralLimiter.type()) {
            key = getIpAddress();
        } else if (LimitType.CUSTOMER == neuralLimiter.type()) {
            key = neuralLimiter.value().length() != 0 ? neuralLimiter.value() : method.getName().toUpperCase();
        } else {
            return pjp.proceed();
        }

        final LimiterContext limiterContext = new LimiterContext();
        return limiter.originalCall(limiterContext, key, pjp::proceed);
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