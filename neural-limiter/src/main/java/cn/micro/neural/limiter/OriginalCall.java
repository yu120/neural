package cn.micro.neural.limiter;

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
     * The process fall back
     *
     * @return object for fallback result
     * @throws Throwable throw fallback exception
     */
    default Object fallback() throws Throwable {
        return null;
    }

}
