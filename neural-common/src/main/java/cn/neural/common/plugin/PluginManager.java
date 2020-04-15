package cn.neural.common.plugin;

import java.io.File;
import java.util.List;

/**
 * PluginManager
 *
 * @author lry
 */
public enum PluginManager implements PluginFactory {

    // ===

    INSTANCE;

    private volatile static PluginManager mgr;
    private PluginClassLoader pluginClassLoader;
    private volatile boolean init;

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void addExternalJar(String basePath) {
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("basePath can not be empty!");
        }
        File dir = new File(basePath);
        if (!dir.exists()) {
            throw new IllegalArgumentException("basePath not exists:" + basePath);
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("basePath must be a directory:" + basePath);
        }

        if (!init) {
            init = true;
            pluginClassLoader = doInit(basePath);
        } else {
            pluginClassLoader.addToClassLoader(basePath, null, true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(String className, Class<T> required) {
        Class<?> cls;
        try {
            cls = pluginClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("can not find class:" + className, e);
        }
        if (required.isAssignableFrom(cls)) {
            try {
                return (T) cls.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("can not newInstance class:" + className, e);
            }
        }

        throw new IllegalArgumentException("class:" + className + " not sub class of " + required);
    }

    @Override
    public Plugin getPlugin(String name) throws Exception {
        return null;
    }

    @Override
    public <T extends Plugin> T getPlugin(Class<T> type) throws Exception {
        return null;
    }

    @Override
    public List<String> getPluginNames() {
        return null;
    }

    @Override
    public boolean hasPlugin(String name) {
        return false;
    }

    @Override
    public void destroy() {

    }

    private synchronized PluginClassLoader doInit(String basePath) {
        return new PluginClassLoader(basePath);
    }

}