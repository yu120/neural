package cn.micro.neural.retryer;

import cn.micro.neural.retryer.support.AttemptTimeLimiters;
import cn.micro.neural.retryer.support.RetryException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AttemptTimeLimiterTest {

    Retryer<Void> r = RetryerBuilder.<Void>newBuilder()
            .withAttemptTimeLimiter(AttemptTimeLimiters.<Void>fixedTimeLimit(1, TimeUnit.SECONDS))
            .build();

    @Test
    public void testAttemptTimeLimit01() throws ExecutionException, RetryException {
    	r.call(new SleepyOut(0L));
        Assert.assertTrue(true);
    }
    
    @Test(expected = UncheckedTimeoutException.class)
    public void testAttemptTimeLimit02() throws Throwable {
        try {
        	SleepyOut sleepyOut = new SleepyOut(10 * 1000L);
            r.call(sleepyOut);
        } catch (Exception e) {
        	throw e.getCause();
        }
    }

    static class SleepyOut implements Callable<Void> {

        final long sleepMs;

        SleepyOut(long sleepMs) {
            this.sleepMs = sleepMs;
        }

        @Override
        public Void call() throws Exception {
            Thread.sleep(sleepMs);
            System.out.println("I'm awake now");
            return null;
        }
    }
}
