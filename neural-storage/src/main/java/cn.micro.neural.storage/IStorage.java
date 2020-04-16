package cn.micro.neural.storage;

import java.util.List;

/**
 * IStorage
 *
 * @author lry
 */
public interface IStorage {

    Number[] eval(String script, List<String> keys, Object... args);

}
