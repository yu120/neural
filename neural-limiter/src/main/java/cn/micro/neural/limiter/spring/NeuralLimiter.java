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
public @interface NeuralLimiter {

    /**
     * Limiter tag
     */
    String value() default "";

    /**
     * The group of service resource
     **/
    String group() default "";

    /**
     * Limiter name
     */
    String name() default "";

    /**
     * Limiter intro
     */
    String intro() default "";

    /**
     * Limiter labels
     */
    String[] labels() default "";

    /**
     * Type of current limit (user-defined key or request ip)
     */
    LimitType type() default LimitType.CUSTOMER;

}
