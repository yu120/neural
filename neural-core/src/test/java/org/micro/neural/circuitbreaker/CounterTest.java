package org.micro.neural.circuitbreaker;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CounterTest {

    final AtomicInteger count = new AtomicInteger(0);

    ExecutorService pool = Executors.newFixedThreadPool(10);

    @After
    public void shutdown() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testRef() {
        Object value = "str";
        change(value);
        System.out.println(value);
    }

    private void change(Object value) {
        value = "str2";
    }

    @Test
    public void concurrent() throws InterruptedException {
        List<Callable<Void>> callables = new ArrayList<Callable<Void>>();
        for (int i = 1; i <= 10; i++) {
            final int idx = i;
            callables.add(new Callable<Void>() {
                public Void call() throws Exception {
                    if (idx % 2 == 0) {
                        count.set(0);
                        System.out.println(Thread.currentThread() + "set 0");
                    } else {
                        int before = count.get();
                        int after = count.incrementAndGet();
                        System.out.println(Thread.currentThread() + ",before:" + before + ",after:" + after);
                    }
                    return null;
                }
            });
        }
        pool.invokeAll(callables);
        System.out.println("final result:" + count.get());
    }

}
