package org.micro.neural.config.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Event Rejected Strategy
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum EventRejectedStrategy {

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
