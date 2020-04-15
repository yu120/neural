package cn.neural.common.validator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * Mobile
 *
 * @author lry
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Mobile.MobileValidator.class)
public @interface Mobile {

    String message() default "手机号格式不正确";

    boolean required() default false;

    boolean blank() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MobileValidator implements ConstraintValidator<Mobile, String> {

        private boolean required;
        private boolean blank;
        private String message;

        /**
         * 正则：手机号（精确）
         * <p>移动：134(0-8)、135、136、137、138、139、147、150、151、152、157、158、159、178、182、183、184、187、188、198</p>
         * <p>联通：130、131、132、145、155、156、175、176、185、186、166</p>
         * <p>电信：133、153、173、177、180、181、189、199</p>
         * <p>全球星：1349</p>
         * <p>虚拟运营商：170</p>
         */
        private static final Pattern MOBILE_PATTERN = Pattern.compile("^(13[0-9]|14[579]|15[0-3,5-9]|16[6]|17[0135678]|18[0-9]|19[89])\\d{8}$");

        public MobileValidator() {
        }

        @Override
        public void initialize(Mobile constraintAnnotation) {
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
                    context.buildConstraintViolationWithTemplate("手机号不能为空").addConstraintViolation();
                    return false;
                }

                return true;
            } else if (value.length() == 0) {
                if (!this.blank) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("手机号不能为空白").addConstraintViolation();
                    return false;
                }

                return true;
            } else if (!MOBILE_PATTERN.matcher(value).matches()) {
                context.disableDefaultConstraintViolation();
                String showMessage = this.message;
                if (showMessage == null || showMessage.length() == 0) {
                    showMessage = "手机号格式不对";
                }

                context.buildConstraintViolationWithTemplate(showMessage).addConstraintViolation();
                return false;
            } else {
                return true;
            }
        }
    }

}
