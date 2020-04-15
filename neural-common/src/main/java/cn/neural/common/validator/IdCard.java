package cn.neural.common.validator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * IdCard
 *
 * @author lry
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD,
        ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Constraint(validatedBy = IdCard.IdCardValidator.class)
public @interface IdCard {

    String message() default "身份证不正确，请核对后重新填写";

    boolean required() default false;

    boolean blank() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class IdCardValidator implements ConstraintValidator<IdCard, String> {

        @Override
        public boolean isValid(String cardNo, ConstraintValidatorContext context) {
            return false;
        }

        /**
         * 15 转换 18身份证补位运算
         */
        private Object transCardLastNo(String oldCardId) {
            char[] ch = oldCardId.toCharArray();
            int[] co = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
            int m = 0;
            for (int i = 0; i < oldCardId.length(); i++) {
                m += (ch[i] - '0') * co[i];
            }

            int residue = m % 11;
            char[] verCode = new char[]{'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
            return String.valueOf(verCode[residue]);
        }

    }

}