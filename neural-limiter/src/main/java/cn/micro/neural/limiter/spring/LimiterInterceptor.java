package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterFactory;
import cn.neural.common.utils.ClassUtils;
import cn.neural.common.utils.CloneUtils;
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
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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

    private static final String DELIMITER = "_";
    private final LimiterFactory limiterFactory = new LimiterFactory();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

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
                limiterConfig.setRate(ruleConfig.getRate());
                limiterConfig.setCounter(ruleConfig.getRequest());
                limiterConfig.setConcurrent(ruleConfig.getConcurrent());
                limiterFactory.addConfig(limiterConfig);
            }
        }
    }

    @Around("execution(public * *(..)) && @annotation(cn.micro.neural.limiter.spring.NeuralLimiter)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = getMethod(pjp, methodSignature);
        NeuralLimiter neuralLimiter = method.getAnnotation(NeuralLimiter.class);

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String tag = neuralLimiter.value().length() != 0 ? neuralLimiter.value() : method.getName().toUpperCase();
        LimiterConfig limiterConfig = limiterFactory.getConfig(neuralLimiter.group(), tag);
        if (limiterConfig == null) {
            return pjp.proceed();
        }

        String tempTag;
        if (LimitType.IP == neuralLimiter.type()) {
            tempTag = tag + DELIMITER + getIpAddress();
        } else {
            tempTag = String.valueOf(parseSpel(method, pjp.getArgs(), tag));
        }

        // 校验新的限流器,不存在则自动创建
        LimiterConfig tempLimiterConfig = CloneUtils.clone(limiterConfig);
        tempLimiterConfig.setTag(tempTag);
        limiterFactory.checkAndAddConfig(tempLimiterConfig);

        // 使用限流器包装调用
        return limiterFactory.originalCall(limiterConfig.identity(), pjp::proceed);
    }

    /**
     * 获取拦截方法
     *
     * @param joinPoint {@link ProceedingJoinPoint}
     * @param signature {@link MethodSignature}
     * @return {@link Method}
     */
    private Method getMethod(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                return joinPoint.getTarget().getClass().getDeclaredMethod(
                        joinPoint.getSignature().getName(), method.getParameterTypes());
            } catch (SecurityException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        return method;
    }

    /**
     * 解析 spel 表达式
     *
     * @param method    方法
     * @param arguments 参数
     * @param spel      表达式
     * @return 执行spel表达式后的结果
     */
    private Object parseSpel(Method method, Object[] arguments, String spel) {
        String[] params = discoverer.getParameterNames(method);
        if (params == null) {
            return spel;
        }

        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < params.length; len++) {
            context.setVariable(params[len], arguments[len]);
        }

        try {
            Expression expression = parser.parseExpression(spel);
            return expression.getValue(context);
        } catch (Exception e) {
            return spel;
        }
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