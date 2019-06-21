package org.micro.neural.extension.singleton;

import org.micro.neural.extension.Extension;

import java.util.concurrent.atomic.AtomicLong;

@Extension("spiSingletonImpl")
public class NpiSingletonImpl implements NpiSingleton {
    private static AtomicLong counter = new AtomicLong(0);
    private long index = 0;

    public NpiSingletonImpl() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        return index;
    }

}
