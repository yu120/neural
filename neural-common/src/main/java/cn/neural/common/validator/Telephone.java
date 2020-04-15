package cn.neural.common.validator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * Telephone
 *
 * @author lry
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Telephone.MobileValidator.class)
public @interface Telephone {

    String message() default "电话号码格式不正确";

    boolean required() default false;

    boolean blank() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MobileValidator implements ConstraintValidator<Telephone, String> {

        private boolean required;
        private boolean blank;
        private String message;

        /**
         * 验证固话号码
         */
        private static final Pattern TEL_PATTERN = Pattern.compile("^(0\\d{2}-\\d{8}(-\\d{1,4})?)|(0\\d{3}-\\d{7,8}(-\\d{1,4})?)$");

        public MobileValidator() {
        }

        @Override
        public void initialize(Telephone constraintAnnotation) {
            this.required = constraintAnnotation.required();
            this.message = constraintAnnotation.message();
            this.blank = constraintAnnotation.blank();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                if (!this.required) {
                    // 禁止默认消息返回
                    context.disableDefaultConstraintViolation();
                    // 自定义返回消息
                    context.buildConstraintViolationWithTemplate("电话号码不能为空").addConstraintViolation();
                    return false;
                } else {
                    return true;
                }
            } else if (value.length() == 0) {
                if (!this.blank) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("电话号码不能为空白").addConstraintViolation();
                    return false;
                } else {
                    return true;
                }
            } else if (!TEL_PATTERN.matcher(value).matches()) {
                context.disableDefaultConstraintViolation();
                String showMessage = this.message;
                if (showMessage == null || showMessage.length() == 0) {
                    showMessage = "电话号码格式不对";
                }

                context.buildConstraintViolationWithTemplate(showMessage).addConstraintViolation();
                return false;
            } else {
                return true;
            }
        }
    }

}
