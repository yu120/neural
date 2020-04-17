package cn.neural.common.utils;

import java.io.*;
import java.util.Collection;

/**
 * CloneUtils
 * <p>
 * Deep cloning of objects and collections.
 *
 * @author lry
 */
public class CloneUtils {

    /**
     * 采用对象的序列化完成对象的深克隆
     *
     * @param obj 待克隆的对象
     * @return {@link T}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T clone(T obj) {
        try {
            // Read object byte data
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                try (ObjectOutputStream obs = new ObjectOutputStream(out)) {
                    //  Write byte stream
                    obs.writeObject(obj);

                    // Allocate memory space, write original objects, generate new objects
                    try (ByteArrayInputStream ios = new ByteArrayInputStream(out.toByteArray())) {
                        try (ObjectInputStream ois = new ObjectInputStream(ios)) {
                            // Return new object and do type conversion
                            return (T) ois.readObject();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Clone object exception", e);
        }
    }

    /**
     * 利用序列化完成集合的深克隆
     *
     * @param collection 待克隆的集合
     * @return {@link Collection<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> Collection<T> clone(Collection<T> collection) {
        try {
            // Read object byte data
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                try (ObjectOutputStream obs = new ObjectOutputStream(out)) {
                    //  Write byte stream
                    obs.writeObject(collection);

                    // Allocate memory space, write original objects, generate new objects
                    try (ByteArrayInputStream ios = new ByteArrayInputStream(out.toByteArray())) {
                        try (ObjectInputStream ois = new ObjectInputStream(ios)) {
                            // Return new object and do type conversion
                            return (Collection<T>) ois.readObject();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Clone collection exception", e);
        }
    }

}
