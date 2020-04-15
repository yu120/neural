package cn.neural.common.plugin;

import java.util.List;

/**
 * PluginFactory
 *
 * @author lry
 */
public interface PluginFactory {

    /**
     * The initialize
     *
     * @throws Exception exception
     */
    void initialize() throws Exception;

    /**
     * The add external jar
     *
     * @param basePath base path
     */
    void addExternalJar(String basePath);

    /**
     * The get plugin
     *
     * @param className class name
     * @param required  required
     * @param <T>       {@link T}
     * @return {@link T}
     */
    <T> T getPlugin(String className, Class<T> required);

    /**
     * The get plugin by name
     *
     * @param name plugin name
     * @return {@link Plugin}
     * @throws Exception exception
     */
    Plugin getPlugin(String name) throws Exception;

    /**
     * The get plugin by class
     *
     * @param pluginClass plugin class
     * @param <T>         {@link T}
     * @return {@link T}
     * @throws Exception exception
     */
    <T extends Plugin> T getPlugin(Class<T> pluginClass) throws Exception;

    /**
     * The get plugin name list
     *
     * @return plugin name list
     */
    List<String> getPluginNames();

    /**
     * The has plugin by name
     *
     * @param name plugin name
     * @return true is has
     */
    boolean hasPlugin(String name);

    /**
     * The destroy
     */
    void destroy();

}
