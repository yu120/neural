package cn.micro.neural.retryer;

import cn.micro.neural.retryer.strategy.BlockStrategies;
import cn.micro.neural.retryer.strategy.BlockStrategy;
import cn.micro.neural.retryer.strategy.StopStrategy;
import cn.micro.neural.retryer.strategy.WaitStrategy;
import cn.micro.neural.retryer.support.Attempt;
import cn.micro.neural.retryer.support.AttemptTimeLimiter;
import cn.micro.neural.retryer.support.AttemptTimeLimiters;
import cn.micro.neural.retryer.support.RetryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A retryer, which executes a call, and retries it until it succeeds, or
 * a stop strategy decides to stop retrying. A wait strategy is used to sleep
 * between attempts. The strategy to decide if the call succeeds or not is
 * also configurable.
 * <p></p>
 * A retryer can also wrap the callable into a RetryerCallable, which can be submitted to an executor.
 * <p></p>
 * Retryer instances are better constructed with a {@link RetryerBuilder}. A retryer
 * is thread-safe, provided the arguments passed to its constructor are thread-safe.
 *
 * @param <V> the type of the call return value
 * @author lry
 */
public final class Retryer<V> {

    private final StopStrategy stopStrategy;
    private final WaitStrategy waitStrategy;
    private final BlockStrategy blockStrategy;
    private final AttemptTimeLimiter<V> attemptTimeLimiter;
    private final Predicate<Attempt<V>> rejectionPredicate;
    private final Collection<RetryListener> listeners;

    public Retryer(StopStrategy stopStrategy, WaitStrategy waitStrategy, Predicate<Attempt<V>> rejectionPredicate) {
        this(AttemptTimeLimiters.<V>noTimeLimit(), stopStrategy, waitStrategy, BlockStrategies.threadSleepStrategy(), rejectionPredicate);
    }

    public Retryer(AttemptTimeLimiter<V> attemptTimeLimiter, StopStrategy stopStrategy,
                   WaitStrategy waitStrategy, Predicate<Attempt<V>> rejectionPredicate) {
        this(attemptTimeLimiter, stopStrategy, waitStrategy, BlockStrategies.threadSleepStrategy(), rejectionPredicate);
    }

    public Retryer(AttemptTimeLimiter<V> attemptTimeLimiter, StopStrategy stopStrategy,
                   WaitStrategy waitStrategy, BlockStrategy blockStrategy, Predicate<Attempt<V>> rejectionPredicate) {
        this(attemptTimeLimiter, stopStrategy, waitStrategy, blockStrategy, rejectionPredicate, new ArrayList<RetryListener>());
    }

    public Retryer(AttemptTimeLimiter<V> attemptTimeLimiter, StopStrategy stopStrategy, WaitStrategy waitStrategy,
                   BlockStrategy blockStrategy, Predicate<Attempt<V>> rejectionPredicate, Collection<RetryListener> listeners) {
        Preconditions.checkNotNull(attemptTimeLimiter, "timeLimiter may not be null");
        Preconditions.checkNotNull(stopStrategy, "stopStrategy may not be null");
        Preconditions.checkNotNull(waitStrategy, "waitStrategy may not be null");
        Preconditions.checkNotNull(blockStrategy, "blockStrategy may not be null");
        Preconditions.checkNotNull(rejectionPredicate, "rejectionPredicate may not be null");
        Preconditions.checkNotNull(listeners, "listeners may not null");

        this.attemptTimeLimiter = attemptTimeLimiter;
        this.stopStrategy = stopStrategy;
        this.waitStrategy = waitStrategy;
        this.blockStrategy = blockStrategy;
        this.rejectionPredicate = rejectionPredicate;
        this.listeners = listeners;
    }

    public V call(Callable<V> callable) throws ExecutionException, RetryException {
        long startTime = System.nanoTime();
        for (int attemptNumber = 1; ; attemptNumber++) {
            Attempt<V> attempt;
            try {
                V result = attemptTimeLimiter.call(callable);
                attempt = new ResultAttempt<>(result, attemptNumber, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            } catch (Throwable t) {
                attempt = new ExceptionAttempt<>(t, attemptNumber, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            }

            for (RetryListener listener : listeners) {
                listener.onRetry(attempt);
            }

            if (!rejectionPredicate.apply(attempt)) {
                return attempt.get();
            }
            if (stopStrategy.shouldStop(attempt)) {
                throw new RetryException(attemptNumber, attempt);
            } else {
                long sleepTime = waitStrategy.computeSleepTime(attempt);
                try {
                    blockStrategy.block(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RetryException(attemptNumber, attempt);
                }
            }
        }
    }

    public RetryerCallable<V> wrap(Callable<V> callable) {
        return new RetryerCallable<V>(this, callable);
    }

    /**
     * The Result Attempt
     *
     * @param <R>
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public static final class ResultAttempt<R> implements Attempt<R> {

        private final R result;
        private final long attemptNumber;
        private final long delaySinceFirstAttempt;

        @Override
        public R get() throws ExecutionException {
            return result;
        }

        @Override
        public boolean hasResult() {
            return true;
        }

        @Override
        public boolean hasException() {
            return false;
        }

        @Override
        public Throwable getExceptionCause() throws IllegalStateException {
            throw new IllegalStateException("The attempt resulted in a result, not in an exception");
        }
    }

    /**
     * The Exception Attempt
     *
     * @param <R>
     * @author lry
     */
    @Getter
    public static final class ExceptionAttempt<R> implements Attempt<R> {

        private final ExecutionException e;
        private final long attemptNumber;
        private final long delaySinceFirstAttempt;

        public ExceptionAttempt(Throwable cause, long attemptNumber, long delaySinceFirstAttempt) {
            this.e = new ExecutionException(cause);
            this.attemptNumber = attemptNumber;
            this.delaySinceFirstAttempt = delaySinceFirstAttempt;
        }

        @Override
        public R get() throws ExecutionException {
            throw e;
        }

        @Override
        public boolean hasResult() {
            return false;
        }

        @Override
        public boolean hasException() {
            return true;
        }

        @Override
        public R getResult() throws IllegalStateException {
            throw new IllegalStateException("The attempt resulted in an exception, not in a result");
        }

        @Override
        public Throwable getExceptionCause() throws IllegalStateException {
            return e.getCause();
        }

    }

    /**
     * The Retryer Callable
     *
     * @param <X>
     * @author lry
     */
    @AllArgsConstructor
    public static class RetryerCallable<X> implements Callable<X> {

        private Retryer<X> retryer;
        private Callable<X> callable;

        @Override
        public X call() throws ExecutionException, RetryException {
            return retryer.call(callable);
        }
    }

}
