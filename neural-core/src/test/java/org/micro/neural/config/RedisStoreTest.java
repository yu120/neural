package org.micro.neural.config;

import org.micro.neural.OriginalCall;
import org.micro.neural.common.URL;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.limiter.Limiter;
import org.micro.neural.limiter.LimiterConfig;

import java.util.List;

public class RedisStoreTest {

    public static void main(String[] args) throws Throwable {
        StorePool storePool = StorePool.getInstance();
        URL url = URL.valueOf("redis://127.0.0.1:6379");
        storePool.initialize(url);
        IStore store = storePool.getStore();

        String key1 = "testQuery";

        Limiter limiter = new Limiter();
        LimiterConfig limiterConfig1 = new LimiterConfig();
        limiter.addConfig(limiterConfig1);
        Object result = limiter.doWrapperCall(key1, new OriginalCall() {
            @Override
            public Object call() throws Throwable {
                System.out.println("Input call");
                return super.call();
            }
        });
    }

}
