package org.micro.neural.bloomfilter;

import org.micro.neural.bloomfilter.core.NeuralBitSet;

import java.util.Arrays;

public class YourBitSet implements NeuralBitSet {

    private int[] data;//boolean array

    public YourBitSet(int size) {
        data = new int[size];
    }

    @Override
    public void set(int bitIndex) {
        data[bitIndex] = 1;
    }

    @Override
    public void set(int bitIndex, boolean value) {
        if (value)
            data[bitIndex] = 1;
        else data[bitIndex] = 0;
    }

    @Override
    public boolean get(int bitIndex) {
        return data[bitIndex] == 1;
    }

    @Override
    public void clear(int bitIndex) {
        data[bitIndex] = 0;
    }

    @Override
    public void clear() {
        Arrays.fill(data, 0);
    }

    @Override
    public long size() {
        long size = 0;
        for (int d : data)
            if (d == 1)
                size++;
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() <= 0;
    }

}
