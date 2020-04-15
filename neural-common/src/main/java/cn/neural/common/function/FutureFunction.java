package cn.neural.common.function;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FutureFunction
 *
 * @param <V>
 * @author lry
 */
@Slf4j
@NoArgsConstructor
public class FutureFunction<R, V> implements Serializable {

    private final Object lock = new Object();
    private volatile FutureState state = FutureState.DOING;
    private final long createTime = System.currentTimeMillis();
    private long processTime;

    private R request = null;
    private V result = null;
    private Throwable cause = null;
    private List<FutureListener<R, V>> listeners;

    public FutureFunction(final R request) {
        this.request = request;
    }

    public R getRequest() {
        return request;
    }

    public long getProcessTime() {
        return processTime;
    }

    /**
     * The success notify
     *
     * @param result {@link V}
     */
    public void onSuccess(V result) {
        this.processTime = System.currentTimeMillis() - createTime;
        this.result = result;
        done();
    }

    /**
     * The failure notify
     *
     * @param cause {@link Throwable}
     */
    public void onFailure(Throwable cause) {
        this.processTime = System.currentTimeMillis() - createTime;
        this.cause = cause;
        done();
    }

    /**
     * The get result or throw {@link Throwable}
     *
     * @return {@link V}
     * @throws Throwable exception {@link Throwable}
     */
    public V get() throws Throwable {
        return get(0);
    }

    /**
     * The get result or throw {@link Throwable}
     *
     * @param timeout wait timeout
     * @return {@link V}
     * @throws Throwable exception {@link Throwable}
     */
    public V get(long timeout) throws Throwable {
        synchronized (lock) {
            if (state != FutureState.DOING) {
                // the get result or throw throwable
                return getResultOrThrowable();
            }

            if (timeout <= 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    cancel(new RuntimeException("Wait interrupted", e));
                }

                // the get result or throw throwable
                return getResultOrThrowable();
            } else {
                long waitTime = timeout - (System.currentTimeMillis() - createTime);
                if (waitTime > 0) {
                    for (; ; ) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException ignore) {
                        }

                        if (state != FutureState.DOING) {
                            break;
                        } else {
                            waitTime = timeout - (System.currentTimeMillis() - createTime);
                            if (waitTime <= 0) {
                                break;
                            }
                        }
                    }
                }

                if (state == FutureState.DOING) {
                    timeoutSoCancel();
                }
            }

            // the get result or throw throwable
            return getResultOrThrowable();
        }
    }

    /**
     * Whether success
     *
     * @return true if the state is {@link FutureState#DONE} and the cause is not null
     */
    public boolean isSuccess() {
        return state == FutureState.DONE && (cause == null);
    }

    /**
     * Whether failure
     *
     * @return true if the state is {@link FutureState#DONE} and the cause is not null
     */
    public boolean isFailure() {
        return state == FutureState.DONE && (cause != null);
    }

    /**
     * Cancel and notify all
     *
     * @return true if cancel success
     */
    public boolean cancel() {
        return cancel(new RuntimeException("Future cancel"));
    }

    /**
     * The add listener
     *
     * @param listener {@link FutureListener}
     */
    public void addListener(FutureListener<R, V> listener) {
        if (listener == null) {
            throw new RuntimeException("Future listener is null");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (state != FutureState.DOING) {
                notifyNow = true;
            } else {
                if (listeners == null) {
                    listeners = new ArrayList<>();
                }

                listeners.add(listener);
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    /**
     * The execute done
     */
    private void done() {
        synchronized (lock) {
            if (state != FutureState.DOING) {
                return;
            }

            state = FutureState.DONE;
            lock.notifyAll();
        }

        // notify all listeners
        notifyListeners();
    }

    /**
     * The execute cancel
     *
     * @param e {@link Exception}
     * @return successful return true
     */
    private boolean cancel(Exception e) {
        this.processTime = System.currentTimeMillis() - createTime;
        synchronized (lock) {
            if (state != FutureState.DOING) {
                return false;
            }

            state = FutureState.CANCELLED;
            cause = e;
            lock.notifyAll();
        }

        // notify all listeners
        notifyListeners();
        return true;
    }

    /**
     * The cancel by timeout
     */
    private void timeoutSoCancel() {
        this.processTime = System.currentTimeMillis() - createTime;
        synchronized (lock) {
            if (state != FutureState.DOING) {
                return;
            }

            state = FutureState.CANCELLED;
            cause = new RuntimeException("Future timeout cancel");
            lock.notifyAll();
        }

        // notify all listeners
        notifyListeners();
    }

    /**
     * The notify all listeners
     */
    private void notifyListeners() {
        if (listeners != null) {
            for (FutureListener<R, V> listener : listeners) {
                notifyListener(listener);
            }
        }
    }

    /**
     * The notify listener
     *
     * @param listener {@link FutureListener}
     */
    private void notifyListener(FutureListener<R, V> listener) {
        try {
            listener.complete(this);
        } catch (Throwable t) {
            log.error("Notify listener exception", t);
        }
    }

    /**
     * The get result or throw throwable
     *
     * @return {@link V}
     * @throws Throwable exception {@link Throwable}
     */
    private V getResultOrThrowable() throws Throwable {
        if (cause != null) {
            throw cause;
        }

        return result;
    }

    /**
     * Listening for success and fail events of future
     *
     * @author lry
     */
    public interface FutureListener<R, V> {

        /**
         * Simple low power operation is recommended
         * <p>
         * 注意反模式：
         * 1.死循环：Future中调用Future
         * 2.耗资源操作或者慢操作
         *
         * @param future {@link FutureFunction}
         * @throws Exception exception {@link Exception}
         */
        void complete(FutureFunction<R, V> future) throws Exception;

    }

    /**
     * FutureState
     *
     * @author lry
     */
    @AllArgsConstructor
    public enum FutureState {

        /**
         * The task is doing
         **/
        DOING,
        /**
         * The task is done
         **/
        DONE,
        /**
         * The task is cancelled
         **/
        CANCELLED;

    }

}

