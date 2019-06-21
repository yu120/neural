package org.micro.neural.limiter;

import java.util.concurrent.TimeUnit;

public class CacheCounterTest {

    public static void main(String[] args) throws Throwable {
        CacheCounter cacheCounter = new CacheCounter(2, TimeUnit.SECONDS);
        cacheCounter.setMaxPermit(10);
        for (int i = 0; i < 100; i++) {
            try {
                if (cacheCounter.tryAcquire()) {
                    System.out.println((i + 1) + "->限流了");
                    continue;
                } else {
                    System.out.println((i + 1) + "->执行业务处理");
                }
            } finally {
                Thread.sleep(20);
            }
        }
    }

}
