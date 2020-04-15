package cn.neural.common.plugin.context;

/**
 * PluginContextListener
 *
 * @author lry
 */
public interface PluginContextListener {

    /**
     * The context initialized
     *
     * @param event {@link PluginContextEvent}
     */
    void contextInitialized(PluginContextEvent event);

    /**
     * The context destroyed
     *
     * @param event {@link PluginContextEvent}
     */
    void contextDestroyed(PluginContextEvent event);

}
