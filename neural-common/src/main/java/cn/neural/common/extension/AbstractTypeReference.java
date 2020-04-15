package cn.neural.common.extension;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * AbstractTypeReference
 *
 * @param <T>
 * @author lry
 */
public abstract class AbstractTypeReference<T> {

    private Class<T> classType;

    @SuppressWarnings("unchecked")
    public AbstractTypeReference() {
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type parameterizedType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
            if (parameterizedType instanceof ParameterizedType) {
                classType = (Class<T>) ((ParameterizedType) parameterizedType).getRawType();
            } else {
                classType = (Class<T>) parameterizedType;
            }
        }
    }

    public Class<T> getClassType() {
        return classType;
    }

}
