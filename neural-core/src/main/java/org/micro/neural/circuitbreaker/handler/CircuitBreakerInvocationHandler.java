package org.micro.neural.circuitbreaker.handler;

import org.micro.neural.circuitbreaker.CircuitBreaker;
import org.micro.neural.circuitbreaker.CircuitBreakerConfig;
import org.micro.neural.circuitbreaker.CircuitBreakerRegister;
import org.micro.neural.circuitbreaker.annotation.GuardByCircuitBreaker;
import org.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * The Circuit Breaker 的jdk代理实现
 *
 * @author lry
 */
@Slf4j
public class CircuitBreakerInvocationHandler implements InvocationHandler {

    private Object target;
    private Class<?> targetClass;

    public CircuitBreakerInvocationHandler(Object target) {
        this.target = target;
        this.targetClass = target.getClass();
    }

    /**
     * 动态生成代理对象
     *
     * @return
     */
    public Object proxy() {
        return Proxy.newProxyInstance(targetClass.getClassLoader(), targetClass.getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        GuardByCircuitBreaker guardByCircuitBreaker = method.getAnnotation(GuardByCircuitBreaker.class);
        if (guardByCircuitBreaker == null) {
            return method.invoke(target, args);
        }

        Class<? extends Throwable>[] noTripExs = guardByCircuitBreaker.noTripExceptions();
        int timeout = guardByCircuitBreaker.timeoutInMs();
        int interval = guardByCircuitBreaker.failCountWindowInMs();
        int failThreshold = guardByCircuitBreaker.failThreshold();

        CircuitBreakerConfig cfg = CircuitBreakerConfig.newDefault();
        if (interval != -1) {
            cfg.setFailCountWindowInMs(interval);
        }
        if (failThreshold != -1) {
            cfg.setFailThreshold(failThreshold);
        }

        String key = targetClass.getSimpleName() + method.getName();
        CircuitBreaker breaker = CircuitBreakerRegister.get(key);
        if (breaker == null) {
            breaker = new CircuitBreaker(key, cfg);
            CircuitBreakerRegister.putIfAbsent(key, breaker);
        }

        Object returnValue = null;

        log.debug("breaker state:{},method:{}", breaker.getState(), method.toGenericString());
        //breaker state
        if (breaker.isOpen()) {
            //判断是否该进入half open状态
            if (breaker.isOpen2HalfOpenTimeout()) {
                //进入half open状态
                breaker.openHalf();
                log.debug("method:{} into half open", method.toGenericString());
                returnValue = processHalfOpen(breaker, method, args, noTripExs);
            } else {
                throw new CircuitBreakerOpenException(method.toGenericString());
            }
        } else if (breaker.isClosed()) {
            try {
                returnValue = method.invoke(target, args);
                //这里看情况是否重置标志
                //breaker.close();
            } catch (Throwable t) {
                if (isNoTripException(t, noTripExs)) {
                    throw t;
                }

                //增加计数
                breaker.incrFailCount();
                if (breaker.isCloseFailThresholdReached()) {
                    //触发阈值，打开
                    log.debug("method:{} reached fail threshold, circuit breaker open",
                            method.toGenericString());
                    breaker.open();
                    throw new CircuitBreakerOpenException(method.toGenericString());
                } else {
                    throw t;
                }
            }
        } else if (breaker.isHalfOpen()) {
            returnValue = processHalfOpen(breaker, method, args, noTripExs);
        }

        return returnValue;
    }

    private Object processHalfOpen(CircuitBreaker breaker, Method method, Object[] args,
                                   Class<? extends Throwable>[] noTripExs) throws Throwable {
        try {
            Object returnValue = method.invoke(target, args);
            breaker.getConsecutiveSuccCount().incrementAndGet();
            if (breaker.isConsecutiveSuccessThresholdReached()) {
                //调用成功则进入close状态
                breaker.close();
            }

            return returnValue;
        } catch (Throwable t) {
            if (isNoTripException(t, noTripExs)) {
                breaker.getConsecutiveSuccCount().incrementAndGet();
                if (breaker.isConsecutiveSuccessThresholdReached()) {
                    breaker.close();
                }
                throw t;
            } else {
                breaker.open();
                throw new CircuitBreakerOpenException(method.toGenericString(), t);
            }
        }
    }

    private boolean isNoTripException(Throwable t, Class<? extends Throwable>[] noTripExceptions) {
        if (noTripExceptions == null || noTripExceptions.length == 0) {
            return false;
        }

        for (Class<? extends Throwable> ex : noTripExceptions) {
            //是否是抛出异常t的父类
            //t java.lang.reflect.InvocationTargetException
            if (ex.isAssignableFrom(t.getCause().getClass())) {
                return true;
            }
        }

        return false;
    }

}
