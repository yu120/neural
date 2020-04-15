package cn.neural.common.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * PluginClassLoader
 *
 * @author lry
 */
@Slf4j
public class PluginClassLoader {

    private URLClassLoader classLoader;

    public PluginClassLoader(String jarfileDir) {
        this(new File(jarfileDir), null);
    }

    public PluginClassLoader(File jarfileDir) {
        this(jarfileDir, null);
    }

    public PluginClassLoader(File jarfileDir, ClassLoader parent) {
        this.classLoader = createClassLoader(jarfileDir, parent);
    }

    public void addToClassLoader(final String baseDir, final FileFilter filter, boolean quiet) {
        File base = new File(baseDir);
        if (base.exists() && base.isDirectory()) {
            File[] files = base.listFiles(filter);
            if (files == null || files.length == 0) {
                if (!quiet) {
                    log.error("No files added to classloader from lib:{}(resolved as:{}).", baseDir, base.getAbsolutePath());
                }
            } else {
                this.classLoader = replaceClassLoader(classLoader, base, filter);
            }
        } else {
            if (!quiet) {
                log.error("Can't find(or read) directory to add to classloader:{}(resolved as:{}).", baseDir, base.getAbsolutePath());
            }
        }
    }

    private URLClassLoader replaceClassLoader(final URLClassLoader oldLoader, final File base, final FileFilter filter) {
        if (null != base && base.canRead() && base.isDirectory()) {
            File[] files = base.listFiles(filter);
            if (null == files || 0 == files.length) {
                log.error("replaceClassLoader base dir:{} is empty", base.getAbsolutePath());
                return oldLoader;
            }
            log.error("replaceClassLoader base dir: {} ,size: {}", base.getAbsolutePath(), files.length);

            URL[] oldElements = oldLoader.getURLs();
            URL[] elements = new URL[oldElements.length + files.length];
            System.arraycopy(oldElements, 0, elements, 0, oldElements.length);

            for (int j = 0; j < files.length; j++) {
                try {
                    URL element = files[j].toURI().normalize().toURL();
                    elements[oldElements.length + j] = element;
                    log.info("Adding '{}' to classloader", element.toString());
                } catch (MalformedURLException e) {
                    log.error("load jar file error", e);
                }
            }

            try {
                oldLoader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ClassLoader oldParent = oldLoader.getParent();
            return URLClassLoader.newInstance(elements, oldParent);
        }

        return oldLoader;
    }

    private URLClassLoader createClassLoader(final File libDir, ClassLoader parent) {
        if (null == parent) {
            parent = Thread.currentThread().getContextClassLoader();
        }

        return replaceClassLoader(URLClassLoader.newInstance(new URL[0], parent), libDir, null);
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

}