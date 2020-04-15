package cn.micro.neural.limiter;

/**
 * ILimiter
 *
 * @author lry
 */
public interface ILimiter {

    /**
     * The limit
     *
     * @param key
     * @param limitCount
     * @param limitPeriod
     * @return
     */
    boolean limit(String key, long limitCount, long limitPeriod);

}
