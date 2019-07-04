package org.micro.neural.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Exception Statistics Counter
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum ExceptionCounter {

    // ===

    TIMEOUT_EXCEPTION("timeout", LongAdder::new),
    REJECTED_EXECUTION_EXCEPTION("rejected", LongAdder::new);

    String key;
    Supplier<LongAdder> supplier;

    public static Map<ExceptionCounter, LongAdder> newInstance() {
        Map<ExceptionCounter, LongAdder> counter = new ConcurrentHashMap<>();
        for (ExceptionCounter e : values()) {
            counter.put(e, e.getSupplier().get());
        }

        return counter;
    }

    public static Map<String, Long> getAndReset(Map<ExceptionCounter, LongAdder> counters) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map.Entry<ExceptionCounter, LongAdder> entry : counters.entrySet()) {
            map.put(entry.getKey().getKey(), entry.getValue().sumThenReset());
        }

        return map;
    }

    public static Map<String, Long> get(Map<ExceptionCounter, LongAdder> counter) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map.Entry<ExceptionCounter, LongAdder> entry : counter.entrySet()) {
            map.put(entry.getKey().getKey(), entry.getValue().sum());
        }

        return map;
    }

    public static ExceptionCounter parse(Throwable t) {
        if (t instanceof TimeoutException) {
            // total all timeout times
            return ExceptionCounter.TIMEOUT_EXCEPTION;
        } else if (t instanceof RejectedExecutionException) {
            // total all rejection times
            return ExceptionCounter.REJECTED_EXECUTION_EXCEPTION;
        }

        return ExceptionCounter.TIMEOUT_EXCEPTION;
    }

}
