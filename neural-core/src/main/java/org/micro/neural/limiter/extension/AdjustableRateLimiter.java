package org.micro.neural.limiter.extension;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;

import org.micro.neural.limiter.extension.SmoothRateLimiter.SmoothBursty;
import org.micro.neural.limiter.extension.SmoothRateLimiter.SmoothWarmingUp;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The Adjustable Rate Limiter.
 *
 * @author lry
 */
public class AdjustableRateLimiter {

    private static final long TIME_GRIT = 1L;
    private volatile double timeGritSecond;

    double getTimeGritSecond() {
        return timeGritSecond;
    }

    private void setTimeGritSecond(long timeGritSecond) {
        this.timeGritSecond = SECONDS.toMicros(timeGritSecond);
    }

    public static AdjustableRateLimiter create(long permitsPerSecond) {
        return create(permitsPerSecond, TIME_GRIT);
    }

    public static AdjustableRateLimiter create(double permitsPerSecond, long timeGritSecond) {
        return create(SleepingStopwatch.createFromSystemTimer(), permitsPerSecond, timeGritSecond);
    }

    /**
     * The create adjustable rate limiter
     *
     * @param stopwatch        {@link SleepingStopwatch}
     * @param permitsPerSecond permits per second
     * @param timeGritSecond   time grit second
     * @return {@link AdjustableRateLimiter}
     */
    private static AdjustableRateLimiter create(SleepingStopwatch stopwatch, double permitsPerSecond, long timeGritSecond) {
        AdjustableRateLimiter adjustableRateLimiter = new SmoothBursty(stopwatch, 1.0);
        adjustableRateLimiter.setTimeGritSecond(timeGritSecond);
        adjustableRateLimiter.setRate(permitsPerSecond);
        return adjustableRateLimiter;
    }

    public static AdjustableRateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        return create(permitsPerSecond, warmupPeriod, unit, TIME_GRIT);
    }

    public static AdjustableRateLimiter create(
            double permitsPerSecond, long warmUpPeriod, TimeUnit unit, long timeGritSecond) {
        if (warmUpPeriod < 0) {
            throw new IllegalArgumentException(String.format("warmupPeriod must not be negative: %s", warmUpPeriod));
        }
        return create(SleepingStopwatch.createFromSystemTimer(),
                permitsPerSecond, warmUpPeriod, unit, 3.0, timeGritSecond);
    }

    /**
     * The create adjustable rate limiter
     *
     * @param stopwatch        {@link SleepingStopwatch}
     * @param permitsPerSecond permits per second
     * @param warmUpPeriod     warm up period
     * @param unit             {@link TimeUnit}
     * @param coldFactor       cold factor
     * @param timeGritSecond   time grit second
     * @return {@link AdjustableRateLimiter}
     */
    private static AdjustableRateLimiter create(SleepingStopwatch stopwatch, double permitsPerSecond,
                                                long warmUpPeriod, TimeUnit unit, double coldFactor, long timeGritSecond) {
        AdjustableRateLimiter adjustableRateLimiter = new SmoothWarmingUp(stopwatch, warmUpPeriod, unit, coldFactor);
        adjustableRateLimiter.setTimeGritSecond(timeGritSecond);
        adjustableRateLimiter.setRate(permitsPerSecond);
        return adjustableRateLimiter;
    }

    private final SleepingStopwatch stopwatch;
    private volatile Object mutexDoNotUseDirectly;

    private Object mutex() {
        Object mutex = mutexDoNotUseDirectly;
        if (mutex == null) {
            synchronized (this) {
                mutex = mutexDoNotUseDirectly;
                if (mutex == null) {
                    mutexDoNotUseDirectly = mutex = new Object();
                }
            }
        }
        return mutex;
    }

    AdjustableRateLimiter(SleepingStopwatch stopwatch) {
        if (stopwatch == null) {
            throw new NullPointerException();
        }
        this.stopwatch = stopwatch;
    }

    public final void setRate(double permitsPerSecond) {
        if (permitsPerSecond <= 0.0 || Double.isNaN(permitsPerSecond)) {
            throw new IllegalArgumentException("rate must be positive");
        }
        synchronized (mutex()) {
            doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }

    void doSetRate(double permitsPerSecond, long nowMicros) {

    }

    public final double getRate() {
        synchronized (mutex()) {
            return doGetRate();
        }
    }

    public double doGetRate() {
        return 0;
    }

    double acquire() {
        return acquire(1);
    }

    double acquire(int permits) {
        long microsToWait = reserve(permits);
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    private long reserve(int permits) {
        checkPermits(permits);
        synchronized (mutex()) {
            return reserveAndGetWaitLength(permits, stopwatch.readMicros());
        }
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int permits) {
        return tryAcquire(permits, 0, MICROSECONDS);
    }

    public boolean tryAcquire() {
        return tryAcquire(1, 0, MICROSECONDS);
    }

    private boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = max(unit.toMicros(timeout), 0);
        checkPermits(permits);
        long microsToWait;

        // 应对并发情况需要同步
        synchronized (mutex()) {
            long nowMicros = stopwatch.readMicros();
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false;
            } else {
                // 获得需要等待的时间
                microsToWait = reserveAndGetWaitLength(permits, nowMicros);
            }
        }

        // 等待，当未达到限制时，microsToWait为0
        stopwatch.sleepMicrosUninterruptibly(microsToWait);

        return true;
    }

    private boolean canAcquire(long nowMicros, long timeoutMicros) {
        return queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros;
    }

    private long reserveAndGetWaitLength(int permits, long nowMicros) {
        long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    /**
     * The query earliest available
     *
     * @param nowMicros now micros
     */
    long queryEarliestAvailable(long nowMicros) {
        return 0L;
    }

    /**
     * The reserve earliest available
     *
     * @param permits   permits
     * @param nowMicros now micros
     */
    long reserveEarliestAvailable(int permits, long nowMicros) {
        return 0L;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "RateLimiter[stableRate=%3.1fqps]", getRate());
    }

    /**
     *
     */
    static class SleepingStopwatch {

        SleepingStopwatch() {
        }

        protected long readMicros() {
            return 0L;
        }

        protected void sleepMicrosUninterruptibly(long micros) {

        }

        static SleepingStopwatch createFromSystemTimer() {
            return new SleepingStopwatch() {
                final Stopwatch stopwatch = Stopwatch.createStarted();

                @Override
                protected long readMicros() {
                    return stopwatch.elapsed(MICROSECONDS);
                }

                @Override
                protected void sleepMicrosUninterruptibly(long micros) {
                    if (micros > 0) {
                        Uninterruptibles.sleepUninterruptibly(micros, MICROSECONDS);
                    }
                }
            };
        }
    }

    private static void checkPermits(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException(String.format("Requested permits (%s) must be positive", permits));
        }
    }

}
