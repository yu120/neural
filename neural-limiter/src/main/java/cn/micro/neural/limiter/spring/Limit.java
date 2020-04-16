package cn.micro.neural.limiter.spring;

import java.lang.annotation.*;

/**
 * Limiter annotation
 *
 * @author lry
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Limit {

    /**
     * Limiter name
     */
    String name() default "";

    /**
     * Limiter intro
     */
    String intro() default "";

    /**
     * Limiter key
     */
    String key() default "";

    /**
     * The rate limit time period range, unit (ms)
     */
    int ratePeriod();

    /**
     * The rate limit maximum number of visits in a certain time
     */
    int rateMax();

    /**
     * Type of current limit (user-defined key or request ip)
     */
    LimitType type() default LimitType.CUSTOMER;

}
