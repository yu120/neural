package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.LimiterFactory;
import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterContext;
import cn.neural.common.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 * LimitInterceptor
 *
 * @author lry
 */
@Slf4j
@Aspect
@Configuration
@EnableConfigurationProperties(LimiterRuleConfig.class)
public class LimiterInterceptor implements ApplicationContextAware {

    private final LimiterFactory limiterFactory = new LimiterFactory();

    @Autowired
    private LimiterRuleConfig limiterRuleConfig;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Map<String, LimiterRuleConfig.RuleProperties>> ruleMap = new HashMap<>();
        limiterRuleConfig.getRules().forEach(ruleConfig -> ruleMap.computeIfAbsent(
                ruleConfig.getGroup(), k -> new HashMap<>()).put(ruleConfig.getTag(), ruleConfig));

        // scanner class list by spring configuration packages
        Set<Class<?>> classSet = ClassUtils.getClasses(AutoConfigurationPackages.get(applicationContext));
        for (Class<?> clazz : classSet) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                NeuralLimiter neuralLimiter = method.getAnnotation(NeuralLimiter.class);
                if (neuralLimiter == null) {
                    continue;
                }

                LimiterConfig limiterConfig = new LimiterConfig();
                // description configuration
                limiterConfig.setTag(neuralLimiter.value().length() != 0 ? neuralLimiter.value() : method.getName().toUpperCase());
                limiterConfig.setGroup(neuralLimiter.group());
                limiterConfig.setName(neuralLimiter.name());
                if (neuralLimiter.labels().length > 0) {
                    limiterConfig.setLabels(Arrays.asList(neuralLimiter.labels()));
                }
                limiterConfig.setIntro(neuralLimiter.intro());
                limiterConfig.setNode(limiterRuleConfig.getNode());
                limiterConfig.setApplication(limiterRuleConfig.getApplication());

                // rule configuration
                Map<String, LimiterRuleConfig.RuleProperties> tempMap = ruleMap.get(neuralLimiter.group());
                if (tempMap == null || tempMap.isEmpty()) {
                    continue;
                }
                LimiterRuleConfig.RuleProperties ruleConfig = tempMap.get(limiterConfig.getTag());
                if (ruleConfig == null) {
                    continue;
                }
                limiterConfig.setEnable(ruleConfig.getEnable());
                limiterConfig.setMode(ruleConfig.getMode());
                limiterConfig.setStrategy(ruleConfig.getStrategy());
                limiterConfig.setRate(ruleConfig.getRate());
                limiterConfig.setRequest(ruleConfig.getRequest());
                limiterConfig.setConcurrent(ruleConfig.getConcurrent());
                limiterFactory.addLimiter(limiterConfig);
            }
        }
    }

    @Around("execution(public * *(..)) && @annotation(cn.micro.neural.limiter.spring.NeuralLimiter)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = pjp.getArgs();
        NeuralLimiter neuralLimiter = method.getAnnotation(NeuralLimiter.class);

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String tag;
        if (LimitType.IP == neuralLimiter.type()) {
            tag = getIpAddress();
        } else if (LimitType.CUSTOMER == neuralLimiter.type()) {
            tag = neuralLimiter.value().length() != 0 ? neuralLimiter.value() : method.getName().toUpperCase();
        } else {
            return pjp.proceed();
        }

        LimiterConfig limiterConfig = limiterFactory.getLimiterConfig(neuralLimiter.group(), tag);
        if (limiterConfig == null) {
            return pjp.proceed();
        }

        final LimiterContext limiterContext = new LimiterContext();
        return limiterFactory.originalCall(limiterContext, limiterConfig.identity(), pjp::proceed);
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