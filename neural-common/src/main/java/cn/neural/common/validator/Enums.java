package cn.neural.common.validator;

import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The Not In Enum Validation Annotation
 *
 * @author lry
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = Enums.EnumsValidator.class)
public @interface Enums {

    /**
     * 对比枚举类型
     *
     * @return Enum Class
     */
    Class<? extends Enum<?>> value();

    /**
     * 是否允许为空
     *
     * @return false表示不允许为空
     */
    boolean allowNull() default false;

    /**
     * 校验值获取方法名称
     *
     * @return 默认使用value字段
     */
    String name() default "getValue";

    /**
     * 校验失败时提示信息
     *
     * @return message
     */
    String message() default "Illegal enum of parameter values";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Enumeration Value Check Annotation Implementation
     *
     * @author lry
     */
    @Slf4j
    class EnumsValidator implements ConstraintValidator<Enums, Object> {

        private boolean allowNull;
        private Set<Object> values = new HashSet<>();

        @Override
        public void initialize(Enums enums) {
            this.allowNull = enums.allowNull();
            Class<?> clz = enums.value();
            Object[] objects = clz.getEnumConstants();

            try {
                Method method = clz.getMethod(enums.name());
                if (Objects.isNull(method)) {
                    throw new IllegalArgumentException("No found method: " + enums.name());
                }

                for (Object obj : objects) {
                    values.add(method.invoke(obj));
                }
            } catch (Exception e) {
                log.error("[处理枚举校验异常]", e);
            }
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
            if (allowNull) {
                return value == null || values.contains(value);
            } else {
                return value != null && values.contains(value);
            }
        }

    }

}