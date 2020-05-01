package cn.micro.neural.idempotent.spring;

import java.lang.annotation.*;

/**
 * NeuralIdempotent
 *
 * @author lry
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NeuralIdempotent {

}
