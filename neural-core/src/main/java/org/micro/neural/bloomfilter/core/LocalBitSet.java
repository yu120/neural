package org.micro.neural.bloomfilter.core;

import org.micro.neural.extension.Extension;

import java.util.BitSet;

/**
 * The stand-alone bloom filter on native java BitSet.
 *
 * @author lry
 */
@Extension("stand-alone")
public class LocalBitSet implements NeuralBitSet {

    private final BitSet bitSet = new BitSet();

    @Override
    public void set(int bitIndex) {
        this.bitSet.set(bitIndex);
    }

    @Override
    public void set(int bitIndex, boolean value) {
        this.bitSet.set(bitIndex, value);
    }

    @Override
    public boolean get(int bitIndex) {
        return this.bitSet.get(bitIndex);
    }

    @Override
    public void clear(int bitIndex) {
        this.bitSet.clear(bitIndex);
    }

    @Override
    public void clear() {
        this.bitSet.clear();
    }

    @Override
    public long size() {
        return this.bitSet.size();
    }

    @Override
    public boolean isEmpty() {
        return this.size() <= 0;
    }

}
