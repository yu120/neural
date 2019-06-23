package org.micro.neural.common.micro;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance Test.
 *
 * @author lry
 */
public class Performance implements Closeable {

    private int threadCount = 16;
    private int loopCount = 1000000;
    private int logInterval = 0;
    private long startTime;
    private AtomicLong counter = new AtomicLong(0);
    private AtomicLong failCounter = new AtomicLong(0);

    public Performance() {
    }

    public Performance(int threadCount, int loopCount, int logInterval) {
        this.threadCount = threadCount;
        this.loopCount = loopCount;
        this.logInterval = logInterval;
    }

    public void run() throws Exception {
        this.startTime = System.currentTimeMillis();
        TaskThread[] taskThreads = new TaskThread[threadCount];
        for (int i = 0; i < taskThreads.length; i++) {
            TaskInThread t = buildTaskInThread();
            t.initialize();
            taskThreads[i] = new TaskThread(t);
        }

        for (TaskThread task : taskThreads) {
            task.start();
        }
        for (TaskThread task : taskThreads) {
            task.join();
            task.taskInThread.destroy();
        }

        System.out.println("===done===");
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * The build task in thread
     *
     * @return {@link TaskInThread}
     */
    public TaskInThread buildTaskInThread() {
        return null;
    }

    /**
     * The Task In Thread
     *
     * @author lry
     */
    public interface TaskInThread {
        /**
         * The initialize of task
         *
         * @throws Exception throw exception
         */
        void initialize() throws Exception;

        /**
         * The execute of task
         *
         * @throws Exception throw exception
         */
        void execute() throws Exception;

        /**
         * The close of task
         *
         * @throws Exception throw exception
         */
        void destroy() throws Exception;
    }

    /**
     * The Task Thread
     *
     * @author lry
     */
    public class TaskThread extends Thread {

        private static final String MONITOR = "Total=%st, TPS=%st/s, Elapsed=%sms, Failed=%st, FailRate=%s%%.";
        private static final String FAILURE = "The total failure(%s) %s of %s request!!!";

        private TaskInThread taskInThread;

        TaskThread(TaskInThread taskInThread) {
            this.taskInThread = taskInThread;
        }

        @Override
        public void run() {
            long logInterval = Performance.this.logInterval;
            if (logInterval <= 0) {
                logInterval = threadCount * loopCount / 10;
            }

            for (int i = 0; i < loopCount; i++) {
                try {
                    long count = counter.incrementAndGet();
                    taskInThread.execute();
                    if (count % logInterval == 0) {
                        long end = System.currentTimeMillis();
                        long total = counter.get();
                        String tps = String.format("%.4f", count * 1000.0 / (end - startTime));
                        String elapsed = String.format("%.4f", (end - startTime) * 1.0 / (counter.get() - failCounter.get()));
                        long failed = failCounter.get();
                        String failRate = String.format("%.4f", failCounter.get() * 1.0 / counter.get() * 100);
                        System.out.println(String.format(MONITOR, total, tps, elapsed, failed, failRate));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failCounter.incrementAndGet();
                    System.out.println(String.format(FAILURE, e.getMessage(), failCounter.get(), counter.get()));
                }
            }
        }
    }

}