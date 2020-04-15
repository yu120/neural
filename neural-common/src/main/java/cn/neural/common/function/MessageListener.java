package cn.neural.common.function;

/**
 * MessageListener
 *
 * @author lry
 */
@FunctionalInterface
public interface MessageListener<T> {

    /**
     * The notify message
     *
     * @param message message
     */
    void onMessage(T message);

}
