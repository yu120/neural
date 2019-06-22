package org.micro.neural.common.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean Copy Utils
 *
 * @author lry
 */
public class BeanUtils {

    /**
     * Using reflection to realize the object to copy
     *
     * @param from Data source
     * @param to   Object container
     */
    public static void copyProperties(Object from, Object to) throws Exception {
        copyPropertiesExclude(from, to, null);
    }

    /**
     * Copy object properties
     *
     * @param from         Data source
     * @param to           Object container
     * @param excludeArray Exclude list of attributes
     * @throws Exception method invoke exception
     */
    public static void copyPropertiesExclude(Object from, Object to, String[] excludeArray) throws Exception {
        List<String> excludesList = null;
        if (excludeArray != null && excludeArray.length > 0) {
            excludesList = Arrays.asList(excludeArray);
        }

        Method[] fromMethods = from.getClass().getDeclaredMethods();
        Method[] toMethods = to.getClass().getDeclaredMethods();
        for (Method fromMethod : fromMethods) {
            handlerCopy(true, excludesList, from, to, fromMethod, toMethods);
        }
    }

    /**
     * Object property value is copied, only the property value of the specified name is copied
     *
     * @param from         Data source
     * @param to           Object container
     * @param includeArray Attributes to be copied
     * @throws Exception method invoke exception
     */
    public static void copyPropertiesInclude(Object from, Object to, String[] includeArray) throws Exception {
        List<String> includesList;
        if (includeArray != null && includeArray.length > 0) {
            includesList = Arrays.asList(includeArray);
        } else {
            return;
        }

        Method[] fromMethods = from.getClass().getDeclaredMethods();
        Method[] toMethods = to.getClass().getDeclaredMethods();
        for (Method fromMethod : fromMethods) {
            handlerCopy(false, includesList, from, to, fromMethod, toMethods);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Object copyMapToObj(Map data, Object obj) {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method method : methods) {
            try {
                if (method.getName().startsWith("set")) {
                    String field = method.getName();
                    field = field.substring(field.indexOf("set") + 3);
                    field = field.toLowerCase().charAt(0) + field.substring(1);
                    Object mapVal = data.get(field);
                    if (mapVal != null) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            Object parameterVal = toConvert(parameterTypes[0], mapVal);
                            method.invoke(obj, parameterVal);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return obj;
    }

    private static void handlerCopy(boolean exclude, List<String> list,
                                    Object from, Object to,
                                    Method fromMethod, Method[] toMethods) throws Exception {
        String fromMethodName = fromMethod.getName();
        boolean startsWithIs = fromMethodName.startsWith("is");
        if (!fromMethodName.contains("query") && !startsWithIs) {
            return;
        }

        int isIndex = startsWithIs ? 2 : 3;
        String excludeMethodName = fromMethodName.substring(isIndex).toLowerCase();
        if (exclude) {
            if (list != null && list.contains(excludeMethodName)) {
                return;
            }
        } else {
            if (!list.contains(excludeMethodName.substring(0, 1).toLowerCase() + excludeMethodName.substring(1))) {
                return;
            }
        }

        String toMethodName = "set" + fromMethodName.substring(isIndex);
        Method toMethod = findMethodByName(toMethods, toMethodName);
        if (toMethod == null) {
            return;
        }

        Object value = fromMethod.invoke(from);
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            Collection newValue = (Collection) value;
            if (newValue.size() <= 0) {
                return;
            }
        }

        toMethod.invoke(to, value);
    }

    private static Method findMethodByName(Method[] methods, String name) {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        return null;
    }

    private static Object toConvert(Class<?> cl, Object val) {
        for (Map.Entry<Class<?>[], BeanCovert<?>> entry : TYPES.entrySet()) {
            Class<?>[] types = entry.getKey();
            for (Class<?> type : types) {
                if (type.isEnum()) {
                    for (Object enumObj : cl.getEnumConstants()) {
                        if (enumObj.toString().equals(String.valueOf(val))) {
                            return enumObj;
                        }
                    }
                } else if (type.equals(cl) || type.getName().equals(cl.getName())) {
                    return entry.getValue().covert(val);
                }
            }
        }

        return String.valueOf(val);
    }

    private static final Map<Class<?>[], BeanCovert<?>> TYPES = new HashMap<>();

    static {
        // boolean、Boolean
        TYPES.put(new Class<?>[]{boolean.class, Boolean.class},
                (BeanCovert<Boolean>) obj -> Boolean.parseBoolean(String.valueOf(obj)));
        // byte、Byte
        TYPES.put(new Class<?>[]{byte.class, Byte.class},
                (BeanCovert<Byte>) obj -> Byte.parseByte(String.valueOf(obj)));
        // short、Short
        TYPES.put(new Class<?>[]{short.class, Short.class},
                (BeanCovert<Boolean>) obj -> Boolean.parseBoolean(String.valueOf(obj)));
        // int、Integer
        TYPES.put(new Class<?>[]{int.class, Integer.class},
                (BeanCovert<Integer>) obj -> Integer.parseInt(String.valueOf(obj)));
        // long、Long
        TYPES.put(new Class<?>[]{long.class, Long.class},
                (BeanCovert<Long>) obj -> Long.parseLong(String.valueOf(obj)));
        // float、Float
        TYPES.put(new Class<?>[]{float.class, Float.class},
                (BeanCovert<Float>) obj -> Float.parseFloat(String.valueOf(obj)));
        // double、Double
        TYPES.put(new Class<?>[]{double.class, Double.class},
                (BeanCovert<Double>) obj -> Double.parseDouble(String.valueOf(obj)));
        // char、Character
        TYPES.put(new Class<?>[]{char.class, Character.class},
                (BeanCovert<Character>) obj -> String.valueOf(obj).charAt(0));
    }

    public interface BeanCovert<T> {

        /**
         * The bean covert
         *
         * @param obj object
         * @return covert type
         */
        T covert(Object obj);

    }

}