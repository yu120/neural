package org.micro.neural.bloomfilter.core;

import org.micro.neural.extension.SPI;

import java.io.Serializable;

/**
 * Base bit set interface.
 * If you want to use your own data structure you can implement this interface.
 *
 * @author lry
 */
@SPI("local")
public interface NeuralBitSet extends Serializable {

    /**
     * Set a single bit in the Bloom filter, value default is true.
     *
     * @param bitIndex bit index.
     */
    void set(int bitIndex);

    /**
     * Set a single bit in the Bloom filter, value is true or false.
     *
     * @param bitIndex bit index.
     * @param value    value true(1) or false(0).
     */
    void set(int bitIndex, boolean value);

    /**
     * Return the bit set used to store the Bloom filter.
     *
     * @param bitIndex bit index.
     * @return the bit set used to store the Bloom filter. true(1),false(0)
     */
    boolean get(int bitIndex);

    /**
     * Clear the bit set on the index, so the bit set value is false on index.
     *
     * @param bitIndex bit index.
     */
    void clear(int bitIndex);

    /**
     * Clear the bit set, so the bit set value is all false.
     */
    void clear();

    /**
     * Returns the number of bits in the Bloom filter.
     *
     * @return the number of bits in the Bloom filter.
     */
    long size();

    /**
     * Returns is the bit set empty, bit set is empty means no any elements added to bit set.
     *
     * @return is the bit set empty.
     */
    boolean isEmpty();

}
