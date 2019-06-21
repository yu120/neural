package org.micro.neural.retryer.strategy;

/**
 * Factory class for {@link BlockStrategy} instances.
 *
 * @author lry
 */
public final class BlockStrategies {

    private static final BlockStrategy THREAD_SLEEP_STRATEGY = new ThreadSleepStrategy();

    private BlockStrategies() {
    }

    public static BlockStrategy threadSleepStrategy() {
        return THREAD_SLEEP_STRATEGY;
    }

    /**
     * The Thread Sleep Strategy
     *
     * @author lry
     */
    private static class ThreadSleepStrategy implements BlockStrategy {

        @Override
        public void block(long sleepTime) throws InterruptedException {
            Thread.sleep(sleepTime);
        }
    }

}