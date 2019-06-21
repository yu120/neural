package org.micro.neural.retryer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.micro.neural.retryer.strategy.StopStrategies;
import org.micro.neural.retryer.support.Attempt;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class StopStrategiesTest {

    @Test
    public void testNeverStop() {
        assertFalse(StopStrategies.neverStop().shouldStop(failedAttempt(3, 6546L)));
    }

    @Test
    public void testStopAfterAttempt() {
        assertFalse(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(2, 6546L)));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(3, 6546L)));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(4, 6546L)));
    }

    @Test
    public void testStopAfterDelayWithMilliseconds() {
        assertFalse(StopStrategies.stopAfterDelay(1000L, TimeUnit.MILLISECONDS).shouldStop(failedAttempt(2, 999L)));
        assertTrue(StopStrategies.stopAfterDelay(1000L, TimeUnit.MILLISECONDS).shouldStop(failedAttempt(2, 1000L)));
        assertTrue(StopStrategies.stopAfterDelay(1000L, TimeUnit.MILLISECONDS).shouldStop(failedAttempt(2, 1001L)));
    }

    @Test
    public void testStopAfterDelayWithTimeUnit() {
        assertFalse(StopStrategies.stopAfterDelay(1, TimeUnit.SECONDS).shouldStop(failedAttempt(2, 999L)));
        assertTrue(StopStrategies.stopAfterDelay(1, TimeUnit.SECONDS).shouldStop(failedAttempt(2, 1000L)));
        assertTrue(StopStrategies.stopAfterDelay(1, TimeUnit.SECONDS).shouldStop(failedAttempt(2, 1001L)));
    }

    public Attempt<Boolean> failedAttempt(long attemptNumber, long delaySinceFirstAttempt) {
        return new Retryer.ExceptionAttempt<Boolean>(new RuntimeException(), attemptNumber, delaySinceFirstAttempt);
    }
}
