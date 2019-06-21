package org.micro.neural.limiter.extension;

import org.junit.Assert;
import org.junit.Test;

public class AdjustableSemaphoreTest {

    @Test
    public void semaphoreTest() throws Exception {
        AdjustableSemaphore semaphore = new AdjustableSemaphore(5);
        for (int i = 0; i < 20; i++) {
            semaphore.tryAcquire();
        }

        // 5个信号量全被占用，所以当前可用的为0
        Assert.assertEquals(0, semaphore.availablePermits());

        // 将信号量显式设置为2，与上一步合并结果(2-5)=-3，表示目前有5个被占用，信号量只有2，所以还有3个欠着待释放
        semaphore.setMaxPermits(2);
        Assert.assertEquals(-3, semaphore.availablePermits());

        // 将信号量显式设置为20，与上一步合并结果(20-2)+(-3)=15个，表示目前还有15个可用
        semaphore.setMaxPermits(20);
        Assert.assertEquals(15, semaphore.availablePermits());

        // 同上，(3-20)+15=-2
        semaphore.setMaxPermits(3);
        Assert.assertEquals(-2, semaphore.availablePermits());

        // 同上，(1-3)-2=-4
        semaphore.setMaxPermits(1);
        Assert.assertEquals(-4, semaphore.availablePermits());

        // 同上，(10-1)-4=5
        semaphore.setMaxPermits(10);
        Assert.assertEquals(5, semaphore.availablePermits());

        // 释放了7个，所以7+5=12，虽然显式设置了信号量为10，但因多release()了两次，所以无意之中隐式增大了信号量
        for (int i = 0; i < 7; i++) {
            semaphore.release();
        }
        Assert.assertEquals(12, semaphore.availablePermits());
    }

}
