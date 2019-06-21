package org.micro.neural.common.collection;

import lombok.*;

import java.io.Serializable;

/**
 * The Key Value Store
 *
 * @param <K> key
 * @param <V> value
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class KeyValueStore<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    private K key;
    private V value;

}
