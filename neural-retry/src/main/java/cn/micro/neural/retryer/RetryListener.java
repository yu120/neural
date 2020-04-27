package cn.micro.neural.retryer;

import cn.micro.neural.retryer.support.Attempt;

/**
 * The Retry Listener
 *
 * @author lry
 */
public interface RetryListener {

    /**
     * The on retry
     *
     * @param attempt
     * @param <V>
     */
    <V> void onRetry(Attempt<V> attempt);

}
