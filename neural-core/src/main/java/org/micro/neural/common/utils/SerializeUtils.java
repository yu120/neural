package org.micro.neural.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Serialize Utils
 *
 * @author lry
 */
public class SerializeUtils {

    public static <T> T deserialize(Class<T> clz, String json) {
        return JSONObject.parseObject(json, clz);
    }

    public static String serialize(Object object) {
        return JSONObject.toJSONString(object);
    }

    public static Map<String, String> parseStringMap(String json) {
        return JSONObject.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    public static Map<String, Object> parseObjMap(String json) {
        return JSONObject.parseObject(json, new TypeReference<Map<String, Object>>() {
        });
    }

    public static Map parseMap(String json) {
        return JSONObject.parseObject(json, Map.class);
    }

    public static List parseList(String json) {
        return JSON.parseArray(json, List.class);
    }

    public static List parseListString(String json) {
        return JSON.parseObject(json, TypeReference.LIST_STRING);
    }

    public static Class<?> newClass(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The illegal class type: " + clazz);
        }
    }

}
