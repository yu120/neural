package cn.micro.neural.idempotent;

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
