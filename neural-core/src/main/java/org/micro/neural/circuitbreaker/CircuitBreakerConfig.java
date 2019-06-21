package org.micro.neural.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Circuit Breaker Config
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerConfig {

    /**
     * closed状态的失败次数阈值
     */
    private int failThreshold = 5;

    /**
     * closed状态的失败计数的时间窗口
     */
    private int failCountWindowInMs = 60 * 1000;

    /**
     * 处于open状态下进入half-open的超时时间
     */
    private int open2HalfOpenTimeoutInMs = 5 * 1000;

    /**
     * half-open状态下成功次数阈值
     */
    private int consecutiveSuccThreshold = 5;

    public static CircuitBreakerConfig newDefault() {
        return new CircuitBreakerConfig();
    }

}
