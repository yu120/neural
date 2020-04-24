package cn.micro.neural.storage;

import java.util.List;

/**
 * IStorage
 *
 * @author lry
 */
public interface IStorage {

    Number[] eval(String script, List<String> keys, Object... args);

    boolean set(String key, Object value);

    boolean setEx(String key, Object value, Long expireTime);

    boolean exists(String key);

    Object get(String key);

    boolean remove(String key);

}
