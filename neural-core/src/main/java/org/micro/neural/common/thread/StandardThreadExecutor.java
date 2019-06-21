package org.micro.neural.common.thread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Standard Thread Executor
 * <p>
 * 设计思路：coreThread -> maxThread -> queue -> reject
 * <p>
 * 场景优势：比较适合于业务处理需要远程资源的场景
 *
 * @author lry
 */
public class StandardThreadExecutor extends ThreadPoolExecutor {

    private static final int DEFAULT_MIN_THREADS = 20;
    private static final int DEFAULT_MAX_THREADS = 200;
    private static final int DEFAULT_MAX_IDLE_TIME = 60 * 1000;

    /**
     * Number of tasks being processed
     */
    private AtomicInteger submittedTasksCount;
    /**
     * Maximum number of tasks allowed to be processed simultaneously
     */
    private int maxSubmittedTaskCount;

    public StandardThreadExecutor() {
        this(DEFAULT_MIN_THREADS, DEFAULT_MAX_THREADS);
    }

    public StandardThreadExecutor(int coreThread, int maxThreads) {
        this(coreThread, maxThreads, maxThreads);
    }

    public StandardThreadExecutor(int coreThread, int maxThreads, long keepAliveTime, TimeUnit unit) {
        this(coreThread, maxThreads, keepAliveTime, unit, maxThreads);
    }

    public StandardThreadExecutor(int coreThreads, int maxThreads, int queueCapacity) {
        this(coreThreads, maxThreads, queueCapacity, Executors.defaultThreadFactory());
    }

    public StandardThreadExecutor(
            int coreThreads, int maxThreads, int queueCapacity, ThreadFactory threadFactory) {
        this(coreThreads, maxThreads, DEFAULT_MAX_IDLE_TIME, TimeUnit.MILLISECONDS, queueCapacity, threadFactory);
    }

    public StandardThreadExecutor(
            int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit, int queueCapacity) {
        this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, Executors.defaultThreadFactory());
    }

    public StandardThreadExecutor(
            int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
            int queueCapacity, ThreadFactory threadFactory) {
        this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, threadFactory, new AbortPolicy());
    }

    public StandardThreadExecutor(
            int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
            int queueCapacity, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(coreThreads, maxThreads, keepAliveTime, unit, new ExecutorQueue(), threadFactory, handler);
        ((ExecutorQueue) getQueue()).setStandardThreadExecutor(this);
        submittedTasksCount = new AtomicInteger(0);

        // 最大并发任务限制： 队列buffer数 + 最大线程数
        maxSubmittedTaskCount = queueCapacity + maxThreads;
    }

    public int getSubmittedTasksCount() {
        return this.submittedTasksCount.get();
    }

    public int getMaxSubmittedTaskCount() {
        return maxSubmittedTaskCount;
    }

    @Override
    public void execute(Runnable command) {
        int count = submittedTasksCount.incrementAndGet();

        // 超过最大的并发任务限制，进行reject, 依赖的LinkedTransferQueue没有长度限制，因此这里进行控制
        if (count > maxSubmittedTaskCount) {
            submittedTasksCount.decrementAndGet();
            getRejectedExecutionHandler().rejectedExecution(command, this);
        }

        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            // there could have been contention around the queue
            if (!((ExecutorQueue) getQueue()).force(command)) {
                submittedTasksCount.decrementAndGet();
                getRejectedExecutionHandler().rejectedExecution(command, this);
            }
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTasksCount.decrementAndGet();
    }

    /**
     * LinkedTransferQueue
     * <p>
     * 能保证更高性能，相比与LinkedBlockingQueue有明显提升,
     * 不过LinkedTransferQueue的缺点是没有队列长度控制，需要在外层协助控制
     *
     * @author lry
     */
    public static class ExecutorQueue extends LinkedTransferQueue<Runnable> {

        private static final long serialVersionUID = -265236426751004839L;

        private StandardThreadExecutor threadPoolExecutor;

        public ExecutorQueue() {
            super();
        }

        public void setStandardThreadExecutor(StandardThreadExecutor threadPoolExecutor) {
            this.threadPoolExecutor = threadPoolExecutor;
        }

        /**
         * 代码来源于 tomcat
         *
         * @param r {@link Runnable}
         * @return true/false
         */
        public boolean force(Runnable r) {
            if (threadPoolExecutor.isShutdown()) {
                throw new RejectedExecutionException(
                        "Executor not running, can't force a command into the queue");
            }

            // forces the item onto the queue, to be used if the task is rejected
            return super.offer(r);
        }

        /**
         * tomcat的代码进行一些小变更
         *
         * @param r {@link Runnable}
         * @return true/false
         */
        @Override
        public boolean offer(Runnable r) {
            int poolSize = threadPoolExecutor.getPoolSize();

            // we are maxed out on threads, simply queue the object
            if (poolSize == threadPoolExecutor.getMaximumPoolSize()) {
                return super.offer(r);
            }

            // we have idle threads, just add it to the queue note that we don't use getActiveCount(), see BZ 49730
            if (threadPoolExecutor.getSubmittedTasksCount() <= poolSize) {
                return super.offer(r);
            }

            // if we have less threads than maximum force creation of a new thread
            if (poolSize < threadPoolExecutor.getMaximumPoolSize()) {
                return false;
            }

            // if we reached here, we need to add it to the queue
            return super.offer(r);
        }
    }

}
