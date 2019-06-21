package org.micro.neural.circuitbreaker;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The Circuit Breaker Register
 *
 * @author lry
 */
public class CircuitBreakerRegister {

    private static ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<String, CircuitBreaker>();

    public static CircuitBreaker get(String key) {
        return breakers.get(key);
    }

    public static void putIfAbsent(String key, CircuitBreaker circuitBreaker) {
        breakers.putIfAbsent(key, circuitBreaker);
    }

}
