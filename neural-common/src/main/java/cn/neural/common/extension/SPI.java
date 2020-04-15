package cn.neural.common.extension;

import java.lang.annotation.*;

/**
 * SPI
 *
 * <font color="red">
 * <b>功能特性：</b><br>
 * 1.支持自定义实现类为单例/多例<br>
 * 2.支持设置默认的实现类<br>
 * 3.支持实现类order排序<br>
 * 4.支持实现类定义特征属性category，用于区分多维度的不同类别<br>
 * 5.支持根据category属性值来搜索实现类<br>
 * 6.支持自动扫描实现类<br>
 * 7.支持手动添加实现类<br>
 * 8.支持获取所有实现类<br>
 * 9.支持只创建所需实现类，解决JDK原生的全量方式<br>
 * 10.支持自定义ClassLoader来加载class<br>
 * </font>
 *
 * @author lry
 * @since 3.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * Default implementation ID
     **/
    String value() default "";

    /**
     * Declares whether a new object needs to be created each time an
     * implementation class is acquired, that is, whether it is singleton
     **/
    boolean single() default false;

}
