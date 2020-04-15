package cn.neural.common.function;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * LambdaFunction
 *
 * @param <T>
 * @param <R>
 * @author lry
 */
@FunctionalInterface
public interface LambdaFunction<T, R> extends Function<T, R>, Serializable {

    Map<String, SerializedLambda> CLASS_LAMBDA_CACHE = new ConcurrentHashMap<>();

    /**
     * Get field name
     *
     * @param lambda {@link LambdaFunction}
     * @param <T>    {@link T}
     * @param <R>    {@link R}
     * @return {@link SerializedLambda}
     */
    static <T, R> String field(LambdaFunction<T, R> lambda) {
        return resolve(lambda).getImplFieldName();
    }

    /**
     * Resolve serialized lambda
     *
     * @param lambda {@link LambdaFunction}
     * @param <T>    {@link T}
     * @param <R>    {@link R}
     * @return {@link SerializedLambda}
     */
    static <T, R> SerializedLambda resolve(LambdaFunction<T, R> lambda) {
        if (!lambda.getClass().isSynthetic()) {
            throw new RuntimeException("This method can only pass a lambda expression synthesis class");
        }

        // read java.lang.invoke.SerializedLambda
        java.lang.invoke.SerializedLambda tempLambda;
        try {
            Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            tempLambda = (java.lang.invoke.SerializedLambda) method.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String key = tempLambda.getImplClass() + "@" + tempLambda.getImplMethodName();
        SerializedLambda serializedLambda = CLASS_LAMBDA_CACHE.get(key);
        if (serializedLambda != null) {
            return serializedLambda;
        }

        // setter
        serializedLambda = new SerializedLambda();
        serializedLambda.setCapturingClassName(tempLambda.getCapturingClass().replace('/', '.'));
        serializedLambda.setFunctionalInterfaceClass(tempLambda.getFunctionalInterfaceClass());
        serializedLambda.setFunctionalInterfaceMethodName(tempLambda.getFunctionalInterfaceMethodName());
        serializedLambda.setFunctionalInterfaceMethodSignature(tempLambda.getFunctionalInterfaceMethodSignature());
        serializedLambda.setImplClassName(tempLambda.getImplClass());
        serializedLambda.setImplMethodName(tempLambda.getImplMethodName());
        serializedLambda.setImplMethodSignature(tempLambda.getImplMethodSignature());
        serializedLambda.setImplMethodKind(tempLambda.getImplMethodKind());
        serializedLambda.setInstantiatedMethodType(tempLambda.getInstantiatedMethodType());

        // setter capturedArgs
        int count = tempLambda.getCapturedArgCount();
        if (count > 0) {
            Object[] capturedArgs = new Object[count];
            for (int i = 0; i < count; i++) {
                capturedArgs[i] = tempLambda.getCapturedArg(i);
            }
            serializedLambda.setCapturedArgs(capturedArgs);
        }

        // setter capturedArgs
        serializedLambda.setFunctionalInterfaceClassName(tempLambda.getFunctionalInterfaceClass().replace('/', '.'));

        // setter instantiatedType
        try {
            serializedLambda.setImplClass(Class.forName((tempLambda.getInstantiatedMethodType()
                    .substring(2, tempLambda.getInstantiatedMethodType().indexOf(';'))).replace("/", ".")));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find the specified class", e);
        }

        // setter field name
        String fieldName = tempLambda.getImplMethodName();
        if (fieldName.startsWith("get") || fieldName.startsWith("set")) {
            fieldName = fieldName.substring(3);
        } else if (fieldName.startsWith("is")) {
            fieldName = fieldName.substring(2);
        }
        if (Character.isLowerCase(fieldName.charAt(0))) {
            serializedLambda.setImplFieldName(fieldName);
        } else {
            serializedLambda.setImplFieldName(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1));
        }

        CLASS_LAMBDA_CACHE.put(key, serializedLambda);
        return serializedLambda;
    }

    /**
     * SerializedLambda
     *
     * @author lry
     */
    @Data
    public static class SerializedLambda implements Serializable {

        private static final long serialVersionUID = 3944979647125031299L;

        private String capturingClassName;
        private String functionalInterfaceClass;
        private String functionalInterfaceMethodName;
        private String functionalInterfaceMethodSignature;
        private String implClassName;
        private String implMethodName;
        private String implMethodSignature;
        private int implMethodKind;
        private String instantiatedMethodType;
        private Object[] capturedArgs;

        private Class<?> implClass;
        private String implFieldName;
        private String functionalInterfaceClassName;

    }

}

