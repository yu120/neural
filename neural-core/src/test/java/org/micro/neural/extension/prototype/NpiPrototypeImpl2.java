package org.micro.neural.extension.prototype;

import org.micro.neural.extension.Extension;

import java.util.concurrent.atomic.AtomicLong;

@Extension("spiPrototypeImpl2")
public class NpiPrototypeImpl2 implements NpiPrototype {
    private static AtomicLong counter = new AtomicLong(0);
    private long index = 0;

    public NpiPrototypeImpl2() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        System.out.println("SpiPrototypeTestImpl_" + index + " say hello");
        return index;
    }

}
