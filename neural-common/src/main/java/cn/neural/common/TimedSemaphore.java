package cn.neural.common;

import lombok.Getter;

import java.util.concurrent.*;

/**
 * A specialized <em>semaphore</em> implementation that provides a number of permits in a given time frame.
 * <p>
 * TimedSemaphore sem = new TimedSemaphore(1, TimeUnit.SECOND, 10);
 *
 * @author lry
 */
@Getter
public class TimedSemaphore {

    /**
     * Constant for a value representing no limit.
     * If the limit is set to a value less or equal this constant,
     * the {@code TimedSemaphore} will be effectively switched off.
     */
    public static final int NO_LIMIT = 0;

    /**
     * Constant for the thread pool size for the executor.
     */
    private static final int THREAD_POOL_SIZE = 1;
    /**
     * The executor service for managing the timer thread.
     */
    private final ScheduledExecutorService executorService;
    /**
     * Stores the period for this timed semaphore.
     */
    private final long period;
    private final TimeUnit unit;
    /**
     * A flag whether the executor service was created by this object.
     */
    private final boolean ownExecutor;
    /**
     * A future object representing the timer task.
     */
    private ScheduledFuture<?> task;
    /**
     * Stores the total number of invocations of the acquire() method.
     */
    private long totalAcquireCount;
    /**
     * The counter for the periods. This counter is increased every time a period ends.
     */
    private long periodCount;
    private int limit;
    /**
     * The current counter.
     */
    private int acquireCount;
    /**
     * The number of invocations of acquire() in the last period.
     */
    private int lastCallsPerPeriod;
    /**
     * A flag whether shutdown() was called.
     */
    private boolean shutdown;


    public TimedSemaphore(long timePeriod, TimeUnit timeUnit, int limit) {
        this(null, timePeriod, timeUnit, limit);
    }

    public TimedSemaphore(ScheduledExecutorService service, long timePeriod, TimeUnit timeUnit, int limit) {
        if (timePeriod < 1) {
            throw new IllegalArgumentException("Time period must be greater than 0!");
        }

        this.period = timePeriod;
        this.unit = timeUnit;
        if (service != null) {
            this.executorService = service;
            this.ownExecutor = false;
        } else {
            ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE, r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("MANAGING_TIMER_THREAD");
                return t;
            });
            timerExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            timerExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            this.executorService = timerExecutor;
            this.ownExecutor = true;
        }

        setLimit(limit);
    }

    /**
     * Returns the limit enforced by this semaphore. The limit determines how
     * many invocations of {@link #acquire()} are allowed within the monitored period.
     *
     * @return the limit
     */
    public final synchronized int getLimit() {
        return limit;
    }

    /**
     * Sets the limit. This is the number of times the {@link #acquire()} method
     * can be called within the time period specified. If this limit is reached,
     * further invocations of {@link #acquire()} will block. Setting the limit
     * to a value &lt;= {@link #NO_LIMIT} will cause the limit to be disabled,
     * i.e. an arbitrary number of{@link #acquire()} invocations is allowed in the time period.
     *
     * @param limit the limit
     */
    public final synchronized void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * Initializes a shutdown. After that the object cannot be used any more.
     * This method can be invoked an arbitrary number of times. All invocations
     * after the first one do not have any effect.
     */
    public synchronized void shutdown() {
        if (!shutdown) {
            if (ownExecutor) {
                // if the executor was created by this instance, it has to be shutdown
                getExecutorService().shutdownNow();
            }
            if (task != null) {
                task.cancel(false);
            }
            shutdown = true;
        }
    }

    /**
     * Tests whether the {@link #shutdown()} method has been called on this object.
     * <p>
     * If this method returns <b>true</b>, this instance cannot be used any longer.
     *
     * @return a flag whether a shutdown has been performed
     */
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    /**
     * Acquires a permit from this semaphore. This method will block if
     * the limit for the current period has already been reached. If
     * {@link #shutdown()} has already been invoked, calling this method will
     * cause an exception. The very first call of this method starts the timer
     * task which monitors the time period set for this {@code TimedSemaphore}.
     * From now on the semaphore is active.
     *
     * @throws InterruptedException  if the thread gets interrupted
     * @throws IllegalStateException if this semaphore is already shut down
     */
    public synchronized void acquire() throws InterruptedException {
        prepareAcquire();
        boolean canPass;
        do {
            canPass = acquirePermit();
            if (!canPass) {
                wait();
            }
        } while (!canPass);
    }

    /**
     * Tries to acquire a permit from this semaphore. If the limit of this semaphore has
     * not yet been reached, a permit is acquired, and this method returns
     * <strong>true</strong>. Otherwise, this method returns immediately with the result
     * <strong>false</strong>.
     *
     * @return <strong>true</strong> if a permit could be acquired; <strong>false</strong> otherwise
     * @throws IllegalStateException if this semaphore is already shut down
     */
    public synchronized boolean tryAcquire() {
        prepareAcquire();
        return acquirePermit();
    }

    /**
     * Returns the number of (successful) acquire invocations during the last
     * period. This is the number of times the {@link #acquire()} method was
     * called without blocking. This can be useful for testing or debugging
     * purposes or to determine a meaningful threshold value. If a limit is set,
     * the value returned by this method won't be greater than this limit.
     *
     * @return the number of non-blocking invocations of the {@link #acquire()} method
     */
    public synchronized int getLastAcquiresPerPeriod() {
        return lastCallsPerPeriod;
    }

    /**
     * Returns the number of invocations of the {@link #acquire()} method for
     * the current period. This may be useful for testing or debugging purposes.
     *
     * @return the current number of {@link #acquire()} invocations
     */
    public synchronized int getAcquireCount() {
        return acquireCount;
    }

    /**
     * Returns the number of calls to the {@link #acquire()} method that can
     * still be performed in the current period without blocking. This method
     * can give an indication whether it is safe to call the {@link #acquire()}
     * method without risking to be suspended. However, there is no guarantee
     * that a subsequent call to {@link #acquire()} actually is not-blocking
     * because in the mean time other threads may have invoked the semaphore.
     *
     * @return the current number of available {@link #acquire()} calls in the
     * current period
     */
    public synchronized int getAvailablePermits() {
        return getLimit() - getAcquireCount();
    }

    /**
     * Returns the average number of successful (i.e. non-blocking)
     * {@link #acquire()} invocations for the entire life-time of this {@code
     * TimedSemaphore}. This method can be used for instance for statistical
     * calculations.
     *
     * @return the average number of {@link #acquire()} invocations per time
     * unit
     */
    public synchronized double getAverageCallsPerPeriod() {
        return periodCount == 0 ? 0 : (double) totalAcquireCount / (double) periodCount;
    }

    /**
     * Starts the timer. This method is called when {@link #acquire()} is called
     * for the first time. It schedules a task to be executed at fixed rate to
     * monitor the time period specified.
     *
     * @return a future object representing the task scheduled
     */
    protected ScheduledFuture<?> startTimer() {
        return getExecutorService().scheduleAtFixedRate(this::endOfPeriod, getPeriod(), getPeriod(), getUnit());
    }

    /**
     * The current time period is finished. This method is called by the timer
     * used internally to monitor the time period. It resets the counter and
     * releases the threads waiting for this barrier.
     */
    private synchronized void endOfPeriod() {
        lastCallsPerPeriod = acquireCount;
        totalAcquireCount += acquireCount;
        periodCount++;
        acquireCount = 0;
        notifyAll();
    }

    /**
     * Prepares an acquire operation.
     * <p>
     * Checks for the current state and starts the internal timer if necessary.
     */
    private void prepareAcquire() {
        if (isShutdown()) {
            throw new IllegalStateException("TimedSemaphore is shut down!");
        }
        if (task == null) {
            task = startTimer();
        }
    }

    /**
     * Internal helper method for acquiring a permit.
     *
     * @return a flag whether a permit could be acquired
     */
    private boolean acquirePermit() {
        if (getLimit() <= NO_LIMIT || acquireCount < getLimit()) {
            acquireCount++;
            return true;
        }

        return false;
    }

}
