package cn.neural.common.utils;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ClassUtils
 *
 * @author lry
 */
public class ClassUtils {

    /**
     * Gets a list of all Classes under the specified package path
     *
     * @param packageNames package name list
     * @return Class list
     */
    public static Set<Class<?>> getClasses(String[] packageNames) {
        return getClasses(Arrays.asList(packageNames));
    }

    /**
     * Gets a list of all Classes under the specified package path
     *
     * @param packageNames package name list
     * @return Class list
     */
    public static Set<Class<?>> getClasses(List<String> packageNames) {
        Set<Class<?>> classes = new HashSet<>();
        for (String packageName : packageNames) {
            Set<Class<?>> tempClasses = getClasses(packageName);
            if (tempClasses.isEmpty()) {
                continue;
            }
            classes.addAll(tempClasses);
        }

        return classes;
    }

    /**
     * Gets a list of all Classes under the specified package path
     *
     * @param packageName package name
     * @return Class list
     */
    public static Set<Class<?>> getClasses(String packageName) {
        return getClasses(true, packageName);
    }

    /**
     * Gets a list of all Classes under the specified package path
     *
     * @param recursive   true is recursive
     * @param packageName package name
     * @return Class list
     */
    public static Set<Class<?>> getClasses(boolean recursive, String packageName) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        String tempPackageName = packageName;
        String packageDirName = tempPackageName.replace('.', '/');
        try {
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
                    findRecursion(tempPackageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.charAt(0) == '/') {
                            name = name.substring(1);
                        }
                        if (name.startsWith(packageDirName)) {
                            int idx = name.lastIndexOf('/');
                            if (idx != -1) {
                                tempPackageName = name.substring(0, idx).replace('/', '.');
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    String className = name.substring(tempPackageName.length() + 1, name.length() - 6);
                                    classes.add(Class.forName(tempPackageName + '.' + className));
                                }
                            }
                        }
                    }
                }
            }

            return classes;
        } catch (Exception e) {
            throw new RuntimeException("The get class by package name is exception", e);
        }
    }

    private static void findRecursion(String packageName, String packagePath,
                                      boolean recursive, Set<Class<?>> classes) throws Exception {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] dirFiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
        if (dirFiles == null) {
            return;
        }

        for (File file : dirFiles) {
            if (file.isDirectory()) {
                findRecursion(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
            }
        }
    }

}
