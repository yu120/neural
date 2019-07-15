package org.micro.neural.bloomfilter;

import java.util.BitSet;

/**
 * Implement bloom filter on native java BitSet.
 *
 * @author lry
 */
public class JavaBitSet implements BaseBitSet {

    private BitSet bitSet;

    public JavaBitSet() {
        this.bitSet = new BitSet();
    }

    public JavaBitSet(BitSet bitSet) {
        if (bitSet == null) {
            this.bitSet = new BitSet();
        } else {
            this.bitSet = bitSet;
        }
    }

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
