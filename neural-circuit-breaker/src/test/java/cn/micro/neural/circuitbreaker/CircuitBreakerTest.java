package cn.micro.neural.circuitbreaker;

import org.junit.Before;
import org.junit.Test;
import org.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import org.micro.neural.circuitbreaker.handler.ProxyFactory;
import org.micro.neural.circuitbreaker.service.DemoService;
import org.micro.neural.circuitbreaker.service.DemoServiceImpl;

import java.util.concurrent.TimeUnit;

public class CircuitBreakerTest {

    DemoService demoService;

    @Before
    public void prepare() {
        demoService = ProxyFactory.proxyBean(new DemoServiceImpl());
    }

    @Test
    public void testSucc() {
        for (int i = 0; i < 100; i++) {
            System.out.println(demoService.getUuid(1));
        }
    }

    @Test(expected = CircuitBreakerOpenException.class)
    public void testOpen() {
        for (int i = 0; i < 6; i++) {
            try {
                demoService.getUuid(2);
            } catch (Exception e) {

            }
        }
        demoService.getUuid(1);
    }

    @Test
    public void testFailWindow() throws InterruptedException {
        for (int i = 0; i < 6; i++) {
            Thread.sleep(13 * 1000);
            try {
                demoService.getUuid(2);
            } catch (Exception e) {

            }
        }
        try {
            demoService.getUuid(2);
        } catch (Exception e) {

        }
    }

    @Test
    public void testHalfOpen2Open() throws InterruptedException {
        for (int i = 0; i < 6; i++) {
            try {
                demoService.getUuid(2);
            } catch (Exception e) {

            }
        }
        //into half open
        TimeUnit.SECONDS.sleep(6);
        try {
            demoService.getUuid(2);
        } catch (Exception e) {

        }
        //into open
    }

    @Test
    public void testHalfOpen2Close() throws InterruptedException {
        for (int i = 0; i < 6; i++) {
            try {
                demoService.getUuid(2);
            } catch (Exception e) {

            }
        }
        //into half open
        TimeUnit.SECONDS.sleep(6);
        for (int i = 0; i < 6; i++) {
            demoService.getUuid(1);
        }
        //into close
    }

    @Test
    public void testNoTripException() {
        for (int i = 0; i < 6; i++) {
            try {
                demoService.illegalEx(2);
            } catch (Exception e) {

            }
        }
        // still close
        try {
            demoService.illegalEx(2);
        } catch (Exception e) {

        }
    }

}
