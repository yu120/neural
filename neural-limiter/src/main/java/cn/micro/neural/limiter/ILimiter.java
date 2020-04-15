package cn.micro.neural.limiter;

/**
 * ILimiter
 *
 * @author lry
 */
public interface ILimiter {

    /**
     * The initialize limiter
     *
     * @param limiterConfig {@link LimiterConfig}
     * @throws Exception exception
     */
    void initialize(LimiterConfig limiterConfig) throws Exception;

    /**
     * The call rate limit
     *
     * @param key         limit key
     * @param maxLimit    max limit count
     * @param limitPeriod limit period
     * @return false means current limit
     */
    boolean callRate(String key, long maxLimit, long limitPeriod);

    /**
     * The destroy limiter
     */
    void destroy();

}
