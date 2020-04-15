package cn.neural.common.extension;

import java.lang.annotation.*;

/**
 * Extension Annotation
 * <p>
 * When SPI has multiple implementations, it can be filtered, sorted and returned according to conditions.
 *
 * @author lry
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extension {

    /**
     * Implementation ID
     **/
    String value();

    /**
     * The smaller the order number, the higher the position in the returned instance list.
     */
    int order() default 20;

    /**
     * SPI category, matching according to category when obtaining SPI list.
     * <p>
     * When there is a search-category to be filtered in category, the matching is successful.
     */
    String[] category() default "";

}
