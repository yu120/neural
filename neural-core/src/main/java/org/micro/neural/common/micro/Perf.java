package org.micro.neural.common.micro;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能测试工具<br>
 *
 * @author lry
 */
public abstract class Perf implements Closeable {

    public int threadCount = 16;
    public int loopCount = 1000000;
    public int logInterval = 0;
    public long startTime;
    public AtomicLong counter = new AtomicLong(0);
    public AtomicLong failCounter = new AtomicLong(0);

    public void run() throws Exception {
        this.startTime = System.currentTimeMillis();
        TaskThread[] tasks = new TaskThread[threadCount];
        for (int i = 0; i < tasks.length; i++) {
            TaskInThread t = buildTaskInThread();
            t.initTask();
            tasks[i] = new TaskThread(t);
        }

        for (TaskThread task : tasks) {
            task.start();
        }
        for (TaskThread task : tasks) {
            task.join();
            task.task.close();
        }

        System.out.println("===done===");
    }

    @Override
    public void close() throws IOException {
    }

    public abstract TaskInThread buildTaskInThread();

    public static abstract class TaskInThread implements Closeable {
        public void initTask() throws Exception {
        }

        public abstract void doTask() throws Exception;

        @Override
        public void close() throws IOException {
        }
    }

    public class TaskThread extends Thread {
        private TaskInThread task;

        public TaskThread(TaskInThread task) {
            this.task = task;
        }

        @Override
        public void run() {
            long logInterval = Perf.this.logInterval;
            if (logInterval <= 0) {
                logInterval = threadCount * loopCount / 10;
            }

            for (int i = 0; i < loopCount; i++) {
                try {
                    long count = counter.incrementAndGet();
                    task.doTask();

                    if (count % logInterval == 0) {
                        long end = System.currentTimeMillis();
                        long total = counter.get();
                        String tps = String.format("%.4f", count * 1000.0 / (end - startTime));
                        String elapsed = String.format("%.4f",
                                (end - startTime) * 1.0 / (counter.get() - failCounter.get()));
                        long failed = failCounter.get();
                        String failRate = String.format("%.4f", failCounter.get() * 1.0 / counter.get() * 100);

                        System.out.println(String.format("Total=%st, TPS=%st/s, Elapsed=%sms, " +
                                "Failed=%st, FailRate=%s%%.", total, tps, elapsed, failed, failRate));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failCounter.incrementAndGet();
                    System.out.println("The total failure(" + e.getMessage() + ") " +
                            failCounter.get() + " of " + counter.get() + " request!!!");
                }
            }
        }
    }

}