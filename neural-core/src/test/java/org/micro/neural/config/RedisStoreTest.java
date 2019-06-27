package org.micro.neural.config;

import org.micro.neural.common.URL;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.limiter.core.ClusterLimiter;

import java.util.ArrayList;
import java.util.List;

public class RedisStoreTest {

    public static void main(String[] args) {
        StorePool storePool = StorePool.getInstance();
        URL url = URL.valueOf("redis://127.0.0.1:6379");
        storePool.initialize(url);
        IStore store = storePool.getStore();
        List<Object> keys = new ArrayList<>();
        keys.add("testQuery");
        keys.add(1);
        keys.add(5);
        List<Object> result = store.eval(ClusterLimiter.CONCURRENT_SCRIPT, 1000L, keys);
        System.out.println(result);
    }

}
