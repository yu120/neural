package org.micro.neural.common.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The MultiHashMap
 *
 * @param <K> key
 * @param <S> sub key
 * @param <V> value
 * @author lry
 */
public class MultiHashMap<K extends Serializable,
        S extends Serializable, V extends Serializable> extends HashMap<Object, Object> {

    private static final long serialVersionUID = -3089161666190291052L;

    public MultiHashMap() {
        super();
    }

    /**
     * Associates the specified value with the specified key and subKey in this
     * map. If the map previously contained a mapping for this key and subKey ,
     * the old value is replaced.
     *
     * @param key    Is a Primary key.
     * @param subKey with which the specified value is to be associated.
     * @param value  to be associated with the specified key and subKey
     * @return previous value associated with specified key and subKey, or null
     * if there was no mapping for key and subKey. A null return can
     * also indicate that the HashMap previously associated null with
     * the specified key and subKey.
     */
    @SuppressWarnings("unchecked")
    public Object put(K key, S subKey, V value) {
        HashMap<S, V> a = (HashMap<S, V>) super.get(key);
        if (a == null) {
            a = new HashMap<>();
            super.put(key, a);
        }

        return a.put(subKey, value);
    }

    /**
     * Returns the value to which this map maps the specified key and subKey.
     * Returns null if the map contains no mapping for this key and subKey. A
     * return value of null does not necessarily indicate that the map contains
     * no mapping for the key and subKey; it's also possible that the map
     * explicitly maps the key to null. The containsKey operation may be used to
     * distinguish these two cases.
     *
     * @param key    whose associated value is to be returned.
     * @param subKey whose associated value is to be returned
     * @return the value to which this map maps the specified key.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public V get(K key, S subKey) {
        HashMap a = (HashMap) super.get(key);
        if (a != null) {
            Object b = a.get(subKey);
            return (V) b;
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Set<S> getSubKeys(K key) {
        HashMap a = (HashMap) super.get(key);
        if (a == null) {
            return null;
        }

        return a.keySet();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Set<S> getSubKeys() {
        Set<S> set = new HashSet<>();
        Collection<Object> subMap = super.values();
        for (Object obj : subMap) {
            HashMap<S, V> a = (HashMap<S, V>) obj;
            if (a.isEmpty()) {
                continue;
            }

            set.addAll(a.keySet());
        }

        return set;
    }

}