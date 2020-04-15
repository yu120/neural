package cn.neural.common.plugin;

/**
 * Plugin
 * <p>
 * https://github.com/TFdream/cherry
 *
 * @author lry
 */
public interface Plugin {

    void initialize() throws Exception;

    void destroy();

}
