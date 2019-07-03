package org.micro.neural.config;

import org.micro.neural.OriginalCall;
import org.micro.neural.common.URL;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.limiter.Limiter;
import org.micro.neural.limiter.LimiterConfig;

public class RedisStoreTest {

    public static void main(String[] args) throws Throwable {
        String key1 = "micro:neural:test";

        Limiter limiter = new Limiter();
        LimiterConfig limiterConfig1 = new LimiterConfig();
        limiterConfig1.setResource("test");
        limiterConfig1.setModel("cluster");
        limiterConfig1.setConcurrentEnable(true);
        limiterConfig1.setRateEnable(false);
        limiterConfig1.setRequestEnable(false);
        limiter.addConfig(limiterConfig1);

        StorePool storePool = StorePool.getInstance();
        URL url = URL.valueOf("redis://127.0.0.1:6379");
        storePool.initialize(url);

        Object result = limiter.originalCall(key1, new OriginalCall() {
            @Override
            public Object call() throws Throwable {
                System.out.println("Input call");
                return "return Input call";
            }
        });
        System.out.println(result);
    }

}
