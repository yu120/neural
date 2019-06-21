package org.micro.neural.common.thread;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.extension.Extension;
import org.micro.neural.extension.ExtensionLoader;
import org.micro.neural.extension.SPI;

import java.util.concurrent.*;

/**
 * Thread Pool Executor Factory
 *
 * @author lry
 */
@Slf4j
public class ThreadPoolExecutorFactory {

    /**
     * The build executor
     *
     * @param threadType    {@link ThreadType}
     * @param prefixName    thread pool name prefix
     * @param coreThread    core thread num
     * @param maxThread     max thread num
     * @param queues        queues size
     * @param keepAliveTime keep alive time
     * @return {@link ThreadPoolExecutor}
     */
    public static ThreadPoolExecutor buildExecutor(
            ThreadType threadType, String prefixName, int coreThread,
            int maxThread, int queues, long keepAliveTime) {
        ExtensionLoader<ThreadPool> extensionLoader = ExtensionLoader.getLoader(ThreadPool.class);
        ThreadPool threadPool = extensionLoader.getExtension(threadType.getValue());
        return threadPool.getExecutor(prefixName, coreThread, maxThread, queues, keepAliveTime);
    }

    @Getter
    @AllArgsConstructor
    public enum ThreadType {

        // ===

        CACHED("cached"), FIXED("fixed"), LIMITED("limited");

        private final String value;

    }

    /**
     * ThreadPool
     *
     * @author lry
     */
    @SPI
    public interface ThreadPool {

        /**
         * The get executor
         *
         * @param prefixName    thread pool name prefix
         * @param coreThread    core thread num
         * @param maxThread     max thread num
         * @param queues        queues size
         * @param keepAliveTime keep alive time
         * @return {@link ThreadPoolExecutor}
         */
        ThreadPoolExecutor getExecutor(
                String prefixName, int coreThread, int maxThread, int queues, long keepAliveTime);

    }

    @Extension("cached")
    public static class CachedThreadPool implements ThreadPool {

        @Override
        public ThreadPoolExecutor getExecutor(
                String prefixName, int coreThread, int maxThread, int queues, long keepAliveTime) {
            BlockingQueue<Runnable> linkedBlockingQueue = queues < 0 ?
                    new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queues);
            BlockingQueue<Runnable> blockingQueue = queues == 0 ? new SynchronousQueue<>() : linkedBlockingQueue;
            return new ThreadPoolExecutor(coreThread, maxThread, keepAliveTime, TimeUnit.MILLISECONDS,
                    blockingQueue, new NamedThreadFactory(prefixName, true),
                    new AbortPolicyWithReport(prefixName));
        }

    }


    /**
     * 此线程池启动时即创建固定大小的线程数，不做任何伸缩，来源于：<code>Executors.newFixedThreadPool()</code>
     *
     * @author lry
     * @see Executors#newFixedThreadPool(int)
     */
    @Extension("fixed")
    public static class FixedThreadPool implements ThreadPool {

        @Override
        public ThreadPoolExecutor getExecutor(
                String prefixName, int coreThread, int maxThread, int queues, long keepAliveTime) {
            BlockingQueue<Runnable> linkedBlockingQueue = queues < 0 ?
                    new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queues);
            BlockingQueue<Runnable> blockingQueue = queues == 0 ? new SynchronousQueue<>() : linkedBlockingQueue;
            return new ThreadPoolExecutor(coreThread, coreThread, 0, TimeUnit.MILLISECONDS,
                    blockingQueue, new NamedThreadFactory(prefixName, true),
                    new AbortPolicyWithReport(prefixName));
        }

    }

    /**
     * This thread pool continues to grow until the upper limit, and does not shrink after growth.
     *
     * @author lry
     */
    @Extension("limited")
    public static class LimitedThreadPool implements ThreadPool {

        @Override
        public ThreadPoolExecutor getExecutor(
                String prefixName, int coreThread, int maxThread, int queues, long keepAliveTime) {
            BlockingQueue<Runnable> linkedBlockingQueue = queues < 0 ?
                    new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(queues);
            BlockingQueue<Runnable> blockingQueue = queues == 0 ? new SynchronousQueue<>() : linkedBlockingQueue;
            return new ThreadPoolExecutor(coreThread, maxThread, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                    blockingQueue, new NamedThreadFactory(prefixName, true),
                    new AbortPolicyWithReport(prefixName));
        }
    }

    /**
     * Abort Policy.
     * Log warn info when abort.
     *
     * @author lry
     */
    public static class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

        private final String threadName;

        private AbortPolicyWithReport(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            String msg = String.format("Thread pool is EXHAUSTED! Thread Name: %s, " +
                            "Pool Size: %d (active: %d, core: %d, max: %d, largest: %d)," +
                            "Task: %d (completed: %d)," +
                            "Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s)!",
                    threadName,
                    e.getPoolSize(),
                    e.getActiveCount(),
                    e.getCorePoolSize(),
                    e.getMaximumPoolSize(),
                    e.getLargestPoolSize(),
                    e.getTaskCount(),
                    e.getCompletedTaskCount(),
                    e.isShutdown(),
                    e.isTerminated(),
                    e.isTerminating());
            log.warn(msg);
            throw new RejectedExecutionException(msg);
        }

    }

}
