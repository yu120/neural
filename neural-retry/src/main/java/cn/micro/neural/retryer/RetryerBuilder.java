package cn.micro.neural.retryer;

import cn.micro.neural.retryer.strategy.*;
import cn.micro.neural.retryer.support.Attempt;
import cn.micro.neural.retryer.support.AttemptTimeLimiter;
import cn.micro.neural.retryer.support.AttemptTimeLimiters;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder used to configure and create a {@link Retryer}.
 *
 * @param <V> result of a {@link Retryer}'s call, the type of the call return value
 * @author lry
 */
public class RetryerBuilder<V> {

    private StopStrategy stopStrategy;
    private WaitStrategy waitStrategy;
    private BlockStrategy blockStrategy;
    private AttemptTimeLimiter<V> attemptTimeLimiter;
    private List<RetryListener> listeners = new ArrayList<>();
    private Predicate<Attempt<V>> rejectionPredicate = Predicates.alwaysFalse();

    private RetryerBuilder() {
    }

    public static <V> RetryerBuilder<V> newBuilder() {
        return new RetryerBuilder<V>();
    }

    public RetryerBuilder<V> withRetryListener(RetryListener listener) {
        Preconditions.checkNotNull(listener, "listener may not be null");

        listeners.add(listener);
        return this;
    }

    public RetryerBuilder<V> withWaitStrategy(WaitStrategy waitStrategy) throws IllegalStateException {
        Preconditions.checkNotNull(waitStrategy, "waitStrategy may not be null");
        Preconditions.checkState(this.waitStrategy == null, "a wait strategy has already been set %s", this.waitStrategy);

        this.waitStrategy = waitStrategy;
        return this;
    }

    public RetryerBuilder<V> withStopStrategy(StopStrategy stopStrategy) throws IllegalStateException {
        Preconditions.checkNotNull(stopStrategy, "stopStrategy may not be null");
        Preconditions.checkState(this.stopStrategy == null, "a stop strategy has already been set %s", this.stopStrategy);

        this.stopStrategy = stopStrategy;
        return this;
    }

    public RetryerBuilder<V> withBlockStrategy(BlockStrategy blockStrategy) throws IllegalStateException {
        Preconditions.checkNotNull(blockStrategy, "blockStrategy may not be null");
        Preconditions.checkState(this.blockStrategy == null, "a block strategy has already been set %s", this.blockStrategy);

        this.blockStrategy = blockStrategy;
        return this;
    }

    public RetryerBuilder<V> withAttemptTimeLimiter(AttemptTimeLimiter<V> attemptTimeLimiter) {
        Preconditions.checkNotNull(attemptTimeLimiter);

        this.attemptTimeLimiter = attemptTimeLimiter;
        return this;
    }

    public RetryerBuilder<V> retryIfException() {
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(Exception.class));
        return this;
    }

    public RetryerBuilder<V> retryIfRuntimeException() {
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(RuntimeException.class));
        return this;
    }

    public RetryerBuilder<V> retryIfExceptionOfType(Class<? extends Throwable> exceptionClass) {
        Preconditions.checkNotNull(exceptionClass, "exceptionClass may not be null");

        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(exceptionClass));
        return this;
    }

    public RetryerBuilder<V> retryIfException(Predicate<Throwable> exceptionPredicate) {
        Preconditions.checkNotNull(exceptionPredicate, "exceptionPredicate may not be null");

        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionPredicate<V>(exceptionPredicate));
        return this;
    }

    public RetryerBuilder<V> retryIfResult(Predicate<V> resultPredicate) {
        Preconditions.checkNotNull(resultPredicate, "resultPredicate may not be null");

        rejectionPredicate = Predicates.or(rejectionPredicate, new ResultPredicate<V>(resultPredicate));
        return this;
    }

    public Retryer<V> build() {
        AttemptTimeLimiter<V> theAttemptTimeLimiter = attemptTimeLimiter == null ? AttemptTimeLimiters.<V>noTimeLimit() : attemptTimeLimiter;
        StopStrategy theStopStrategy = stopStrategy == null ? StopStrategies.neverStop() : stopStrategy;
        WaitStrategy theWaitStrategy = waitStrategy == null ? WaitStrategies.noWait() : waitStrategy;
        BlockStrategy theBlockStrategy = blockStrategy == null ? BlockStrategies.threadSleepStrategy() : blockStrategy;

        return new Retryer<V>(theAttemptTimeLimiter, theStopStrategy, theWaitStrategy, theBlockStrategy, rejectionPredicate, listeners);
    }

    /**
     * The Exception Class Predicate
     *
     * @param <V>
     * @author lry
     */
    @AllArgsConstructor
    private static final class ExceptionClassPredicate<V> implements Predicate<Attempt<V>> {

        private Class<? extends Throwable> exceptionClass;

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasException()) {
                return false;
            }

            return exceptionClass.isAssignableFrom(attempt.getExceptionCause().getClass());
        }
    }

    /**
     * The Result Predicate
     *
     * @param <V>
     * @author lry
     */
    @AllArgsConstructor
    private static final class ResultPredicate<V> implements Predicate<Attempt<V>> {

        private Predicate<V> delegate;

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasResult()) {
                return false;
            }
            V result = attempt.getResult();
            return delegate.apply(result);
        }
    }

    /**
     * The Exception Predicate
     *
     * @param <V>
     * @author lry
     */
    @AllArgsConstructor
    private static final class ExceptionPredicate<V> implements Predicate<Attempt<V>> {

        private Predicate<Throwable> delegate;

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasException()) {
                return false;
            }
            return delegate.apply(attempt.getExceptionCause());
        }
    }

}
