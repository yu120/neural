package cn.micro.neural.retryer.support;

import java.util.concurrent.ExecutionException;

/**
 * An attempt of a call, which resulted either in a result returned by the call,
 * or in a Throwable thrown by the call.
 *
 * @param <V> The type returned by the wrapped callable.
 * @author lry
 */
public interface Attempt<V> {

    /**
     * Returns the result of the attempt, if any.
     *
     * @return the result of the attempt
     * @throws ExecutionException
     */
    V get() throws ExecutionException;

    /**
     * Tells if the call returned a result or not
     *
     * @return <code>true</code> if the call returned a result, <code>false</code>
     *         if it threw an exception
     */
    boolean hasResult();

    /**
     * Tells if the call threw an exception or not
     *
     * @return <code>true</code> if the call threw an exception, <code>false</code>
     *         if it returned a result
     */
    boolean hasException();

    /**
     * Gets the result of the call
     *
     * @return the result of the call
     * @throws IllegalStateException
     */
    V getResult() throws IllegalStateException;

    /**
     * Gets the exception thrown by the call
     *
     * @return the exception thrown by the call
     * @throws IllegalStateException
     */
    Throwable getExceptionCause() throws IllegalStateException;

    /**
     * The number, starting from 1, of this attempt.
     *
     * @return the attempt number
     */
    long getAttemptNumber();

    /**
     * The delay since the start of the first attempt, in milliseconds.
     *
     * @return the delay since the start of the first attempt, in milliseconds
     */
    long getDelaySinceFirstAttempt();
    
}
