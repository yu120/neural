package cn.neural.common.monitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

/**
 * A runnable that spawns a monitoring thread triggering any
 * registered {@link FileAlterationObserver} at a specified interval.
 *
 * @author lry
 */
public class FileAlterationMonitor implements Runnable {

    private final long milliseconds;
    private final List<FileAlterationObserver> observers = new CopyOnWriteArrayList<>();
    private Thread thread = null;
    private ThreadFactory threadFactory;
    private volatile boolean running = false;

    /**
     * Construct a monitor with a default interval of 10 seconds.
     */
    public FileAlterationMonitor() {
        this(10000);
    }

    /**
     * Construct a monitor with the specified interval.
     *
     * @param milliseconds The amount of time in milliseconds to wait between checks of the file system
     */
    public FileAlterationMonitor(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * Construct a monitor with the specified interval and set of observers.
     *
     * @param milliseconds The amount of time in milliseconds to wait between checks of the file system
     * @param observers    The set of observers to add to the monitor.
     */
    public FileAlterationMonitor(long milliseconds, FileAlterationObserver... observers) {
        this(milliseconds);
        if (observers != null) {
            for (final FileAlterationObserver observer : observers) {
                addObserver(observer);
            }
        }
    }

    /**
     * Return the interval.
     *
     * @return the interval
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    /**
     * Set the thread factory.
     *
     * @param threadFactory the thread factory
     */
    public synchronized void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Add a file system observer to this monitor.
     *
     * @param observer The file system observer to add
     */
    public void addObserver(FileAlterationObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Remove a file system observer from this monitor.
     *
     * @param observer The file system observer to remove
     */
    public void removeObserver(FileAlterationObserver observer) {
        if (observer != null) {
            while (observers.remove(observer)) {
            }
        }
    }

    /**
     * Returns the set of {@link FileAlterationObserver} registered with
     * this monitor.
     *
     * @return The set of {@link FileAlterationObserver}
     */
    public Iterable<FileAlterationObserver> getObservers() {
        return observers;
    }

    /**
     * Start monitoring.
     *
     * @throws Exception if an error occurs initializing the observer
     */
    public synchronized void start() throws Exception {
        if (running) {
            throw new IllegalStateException("Monitor is already running");
        }
        for (FileAlterationObserver observer : observers) {
            observer.initialize();
        }
        running = true;
        if (threadFactory != null) {
            thread = threadFactory.newThread(this);
        } else {
            thread = new Thread(this);
        }
        thread.start();
    }

    /**
     * Stop monitoring.
     *
     * @throws Exception if an error occurs initializing the observer
     */
    public synchronized void stop() throws Exception {
        stop(milliseconds);
    }

    /**
     * Stop monitoring.
     *
     * @param stopInterval the amount of time in milliseconds to wait for the thread to finish.
     *                     A value of zero will wait until the thread is finished (see {@link Thread#join(long)}).
     * @throws Exception if an error occurs initializing the observer
     * @since 2.1
     */
    public synchronized void stop(long stopInterval) throws Exception {
        if (!running) {
            throw new IllegalStateException("Monitor is not running");
        }
        running = false;
        try {
            thread.join(stopInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (FileAlterationObserver observer : observers) {
            observer.destroy();
        }
    }

    @Override
    public void run() {
        while (running) {
            for (FileAlterationObserver observer : observers) {
                observer.checkAndNotify();
            }
            if (!running) {
                break;
            }
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
