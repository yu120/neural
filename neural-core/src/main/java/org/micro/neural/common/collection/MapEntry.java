package org.micro.neural.common.collection;

import lombok.*;

import java.util.Map;

/**
 * Map Entry
 *
 * @param <K>
 * @param <V>
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MapEntry<K, V> implements Map.Entry<K, V> {

    private K key;
    private V value;

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        this.value = value;
        return value;
    }

}
