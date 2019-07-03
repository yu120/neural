package org.micro.neural.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Exception Statistics
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum ExceptionStatistics {

    // ===

    TIMEOUT_EXCEPTION("timeout", LongAdder::new),
    REJECTED_EXECUTION_EXCEPTION("rejected", LongAdder::new),
    SQL_EXCEPTION("sql_exception", LongAdder::new),
    RUNTIME_EXCEPTION("runtime_exception", LongAdder::new);

    String key;
    Supplier<LongAdder> supplier;

    public static Map<ExceptionStatistics, LongAdder> build() {
        Map<ExceptionStatistics, LongAdder> counter = new ConcurrentHashMap<>();
        for (ExceptionStatistics e : values()) {
            counter.put(e, e.getSupplier().get());
        }

        return counter;
    }

    public static Map<String, Long> getAndReset(Map<ExceptionStatistics, LongAdder> counters) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map.Entry<ExceptionStatistics, LongAdder> entry : counters.entrySet()) {
            map.put(entry.getKey().getKey(), entry.getValue().sumThenReset());
        }

        return map;
    }

    public static Map<String, Long> get(Map<ExceptionStatistics, LongAdder> counter) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map.Entry<ExceptionStatistics, LongAdder> entry : counter.entrySet()) {
            map.put(entry.getKey().getKey(), entry.getValue().sum());
        }

        return map;
    }

    public static ExceptionStatistics parse(Throwable t) {
        if (t instanceof TimeoutException) {
            // total all timeout times
            return ExceptionStatistics.TIMEOUT_EXCEPTION;
        } else if (t instanceof RejectedExecutionException) {
            // total all rejection times
            return ExceptionStatistics.REJECTED_EXECUTION_EXCEPTION;
        } else if (t instanceof SQLException) {
            // total all sql exception times
            return ExceptionStatistics.SQL_EXCEPTION;
        } else if (t instanceof RuntimeException) {
            // total all runtime exception times
            return ExceptionStatistics.RUNTIME_EXCEPTION;
        }

        return ExceptionStatistics.TIMEOUT_EXCEPTION;
    }

}
