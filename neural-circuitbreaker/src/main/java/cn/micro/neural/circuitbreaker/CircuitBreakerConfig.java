package cn.micro.neural.circuitbreaker;

import lombok.Data;

import java.io.Serializable;

/**
 * CircuitBreakerConfig
 * <p>
 * 1.默认为closed状态
 * 2.在closed状态下，时间窗口failCountWindowInMs范围内，连续失败failThreshold次,则打开熔断为open状态
 * 3.在open状态下，经过open2HalfOpenTimeoutInMs长的超时等待后(熔断休眠时间)，状态将自动进入half-open状态中
 * 4.在half-open状态下，连续经过consecutiveSuccessThreshold此成功后，状态自动变为closed状态，否则失败一次则重置计数
 *
 * @author lry
 */
@Data
public class CircuitBreakerConfig implements Serializable {

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
    private int consecutiveSuccessThreshold = 5;

}
