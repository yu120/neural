package org.micro.neural.config.event;

import lombok.*;

import java.io.Serializable;
import java.util.concurrent.*;

/**
 * The Event Config.
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EventConfig implements Serializable {

    private static final long serialVersionUID = -201057350875510362L;

    public static final String IDENTITY = "event";
    
    /**
     * The thread core num of event
     */
    private Integer coreThread = 1;
    /**
     * The thread max num of event
     */
    private Integer maxThread = 3;
    /**
     * when the number of threads is greater than the core,
     * this is the maximum time that excess idle threads
     * will wait for new tasks before terminating.
     */
    private Long keepAliveTime = 60L;
    /**
     * The thread capacity of event
     */
    private Integer capacity = 1000;
    /**
     * The thread name
     */
    private String threadName = "neural-event-thread";
    /**
     * The log name
     */
    private String logName = "neural-event-log";

    /**
     * The print log data format
     */
    private boolean jsonLog = false;
    /**
     * The collect strategy, default is 'log', optional: log/redis
     */
    private String collect = "log";
    /**
     * The thread rejected strategy of event, default is {@link RejectedStrategy#DISCARD_OLDEST_POLICY}
     */
    private RejectedStrategy rejected = RejectedStrategy.DISCARD_OLDEST_POLICY;

    /**
     * Event Rejected Strategy
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum RejectedStrategy {

        // ====

        /**
         * A handler for rejected tasks that throws a
         * {@code RejectedExecutionException}.
         */
        ABORT_POLICY(new ThreadPoolExecutor.AbortPolicy()),

        /**
         * A handler for rejected tasks that silently discards the
         * rejected task.
         */
        DISCARD_POLICY(new ThreadPoolExecutor.DiscardPolicy()),
        /**
         * A handler for rejected tasks that discards the oldest unhandled
         * request and then retries {@code execute}, unless the executor
         * is shut down, in which case the task is discarded.
         */
        DISCARD_OLDEST_POLICY(new ThreadPoolExecutor.DiscardOldestPolicy()),

        /**
         * A handler for rejected tasks that runs the rejected task
         * directly in the calling thread of the {@code execute} method,
         * unless the executor has been shut down, in which case the task
         * is discarded.
         */
        CALLER_RUNS_POLICY(new ThreadPoolExecutor.CallerRunsPolicy());

        private RejectedExecutionHandler strategy;

    }

}
