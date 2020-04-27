package cn.micro.neural.circuitbreaker;

/**
 * The Original Call.
 *
 * @author lry
 */
public interface OriginalCall {

    /**
     * The process original call
     *
     * @return object for original call result
     * @throws Throwable throw original call exception
     */
    Object call() throws Throwable;

    /**
     * The process original call
     *
     * @param limiterContext {@link LimiterContext}
     * @return object for original call result
     * @throws Throwable throw original call exception
     */
    default Object call(LimiterContext limiterContext) throws Throwable {
        return call();
    }

    /**
     * The process fall back
     *
     * @return object for fallback result
     * @throws Throwable throw fallback exception
     */
    default Object fallback() throws Throwable {
        return null;
    }

    /**
     * The process fall back
     *
     * @param limiterContext {@link LimiterContext}
     * @return object for fallback result
     * @throws Throwable throw fallback exception
     */
    default Object fallback(LimiterContext limiterContext) throws Throwable {
        return fallback();
    }

}
