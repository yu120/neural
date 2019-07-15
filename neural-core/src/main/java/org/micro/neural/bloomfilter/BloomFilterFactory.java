package org.micro.neural.bloomfilter;

import org.micro.neural.bloomfilter.core.NeuralBitSet;
import org.micro.neural.common.utils.MessageDigestUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * The Bloom Filter
 *
 * @param <E> Element type
 * @author lry
 */
public class BloomFilterFactory<E> implements Serializable {

    private int bitSetSize;
    private double bitsPerElement;
    private NeuralBitSet bitSet;
    /**
     * expected (maximum) number of elements to be added
     */
    private int expectedNumberOfFilterElements;
    /**
     * number of elements actually added to the Bloom filter
     */
    private int numberOfAddedElements;
    /**
     * number of hash functions
     */
    private int k;

    /**
     * Bind a bit set for Bloom filter.
     * It can be any data structure that implements the NeuralBitSet interface.
     *
     * @param bitSet {@link NeuralBitSet}
     */
    public void bind(NeuralBitSet bitSet) {
        this.bitSet = bitSet;
    }

    /**
     * Constructs an empty Bloom filter.
     * The total length of the Bloom filter will be c*n.
     *
     * @param c is the number of bits used per element.
     * @param n is the expected number of elements the filter will contain.
     * @param k is the number of hash functions used.
     */
    public BloomFilterFactory(double c, int n, int k) {
        this.expectedNumberOfFilterElements = n;
        this.k = k;
        this.bitsPerElement = c;
        this.bitSetSize = (int) Math.ceil(c * n);
        numberOfAddedElements = 0;
    }

    /**
     * Constructs an empty Bloom filter.
     * The optimal number of hash functions (k) is estimated from the total size of the Bloom
     * and the number of expected elements.
     *
     * @param bitSetSize               defines how many bits should be used in total for the filter.
     * @param expectedNumberOfElements defines the maximum number of elements the filter is expected to contain.
     */
    public BloomFilterFactory(int bitSetSize, int expectedNumberOfElements) {
        this(bitSetSize / (double) expectedNumberOfElements,
                expectedNumberOfElements,
                (int) Math.round((bitSetSize / (double) expectedNumberOfElements) * Math.log(2.0)));
    }

    /**
     * Constructs an empty Bloom filter with a given false positive probability.
     * The number of bits per element and the number of hash functions is estimated to match the false positive probability.
     *
     * @param falsePositiveProbability is the desired false positive probability.
     * @param expectedNumberOfElements is the expected number of elements in the Bloom filter.
     */
    public BloomFilterFactory(double falsePositiveProbability, int expectedNumberOfElements) {
        // c = k / ln(2)
        this(Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2.0))) / Math.log(2.0),
                expectedNumberOfElements,
                // k = ceil(-log_2(false prob.))
                (int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2.0))));
    }

    /**
     * Construct a new Bloom filter based on existing Bloom filter data.
     *
     * @param bitSetSize                     defines how many bits should be used for the filter.
     * @param expectedNumberOfFilterElements defines the maximum number of elements the filter is expected to contain.
     * @param actualNumberOfFilterElements   specifies how many elements have been inserted into the <code>filterData</code> BitSet.
     * @param filterData                     a BitSet representing an existing Bloom filter.
     */
    public BloomFilterFactory(int bitSetSize, int expectedNumberOfFilterElements, int actualNumberOfFilterElements, NeuralBitSet filterData) {
        this(bitSetSize, expectedNumberOfFilterElements);
        this.bitSet = filterData;
        this.numberOfAddedElements = actualNumberOfFilterElements;
    }

    /**
     * Compares the contents of two instances to see if they are equal.
     *
     * @param obj is the object to compare to.
     * @return True if the contents of the objects are equal.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BloomFilterFactory<E> other = (BloomFilterFactory<E>) obj;
        if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements) {
            return false;
        }
        if (this.k != other.k) {
            return false;
        }
        if (this.bitSetSize != other.bitSetSize) {
            return false;
        }
        if (this.bitSet == other.bitSet) {
            return true;
        }

        return this.bitSet != null && this.bitSet.equals(other.bitSet);
    }

    /**
     * Calculates a hash code for this class.
     *
     * @return hash code representing the contents of an instance of this class.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + (this.bitSet != null ? this.bitSet.hashCode() : 0);
        hash = 61 * hash + this.expectedNumberOfFilterElements;
        hash = 61 * hash + this.bitSetSize;
        hash = 61 * hash + this.k;
        return hash;
    }

    /**
     * Calculates the expected probability of false positives based on
     * the number of expected filter elements and the size of the Bloom filter.
     * <p>
     * The value returned by this method is the <i>expected</i> rate of false
     * positives, assuming the number of inserted elements equals the number of
     * expected elements. If the number of elements in the Bloom filter is less
     * than the expected value, the true probability of false positives will be lower.
     *
     * @return expected probability of false positives.
     */
    public double expectedFalsePositiveProbability() {
        return getFalsePositiveProbability(expectedNumberOfFilterElements);
    }

    /**
     * Calculate the probability of a false positive given the specified
     * number of inserted elements.
     *
     * @param numberOfElements number of inserted elements.
     * @return probability of a false positive.
     */
    public double getFalsePositiveProbability(double numberOfElements) {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow((1 - Math.exp(-k * (double) numberOfElements / (double) bitSetSize)), k);
    }

    /**
     * Get the current probability of a false positive. The probability is calculated from
     * the size of the Bloom filter and the current number of elements added to it.
     *
     * @return probability of false positives.
     */
    public double getFalsePositiveProbability() {
        return getFalsePositiveProbability(numberOfAddedElements);
    }

    /**
     * Returns the value chosen for K.
     * <p>
     * K is the optimal number of hash functions based on the size
     * of the Bloom filter and the expected number of inserted elements.
     *
     * @return optimal k.
     */
    public int getK() {
        return k;
    }

    /**
     * Sets all bits to false in the Bloom filter.
     */
    public void clear() {
        bitSet.clear();
        numberOfAddedElements = 0;
    }

    /**
     * Adds an object to the Bloom filter. The output from the object's
     * toString() method is used as input to the hash functions.
     *
     * @param element is an element to register in the Bloom filter.
     */
    public void add(E element) {
        add(element.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds an array of bytes to the Bloom filter.
     *
     * @param bytes array of bytes to add to the Bloom filter.
     */
    public void add(byte[] bytes) {
        int[] hashes = MessageDigestUtils.createHashes(bytes, k);
        for (int hash : hashes) {
            bitSet.set(Math.abs(hash % bitSetSize), true);
        }

        numberOfAddedElements++;
    }

    /**
     * Adds all elements from a Collection to the Bloom filter.
     *
     * @param c Collection of elements.
     */
    public void addAll(Collection<? extends E> c) {
        for (E element : c) {
            add(element);
        }
    }

    /**
     * Returns true if the element could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param element element to check.
     * @return true if the element could have been inserted into the Bloom filter.
     */
    public boolean contains(E element) {
        return contains(element.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns true if the array of bytes could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param bytes array of bytes to check.
     * @return true if the array could have been inserted into the Bloom filter.
     */
    public boolean contains(byte[] bytes) {
        int[] hashes = MessageDigestUtils.createHashes(bytes, k);
        for (int hash : hashes) {
            if (!bitSet.get(Math.abs(hash % bitSetSize))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if all the elements of a Collection could have been inserted
     * into the Bloom filter. Use getFalsePositiveProbability() to calculate the
     * probability of this being correct.
     *
     * @param c elements to check.
     * @return true if all the elements in c could have been inserted into the Bloom filter.
     */
    public boolean containsAll(Collection<? extends E> c) {
        for (E element : c) {
            if (!contains(element)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Read a single bit from the Bloom filter.
     *
     * @param bit the bit to read.
     * @return true if the bit is set, false if it is not.
     */
    public boolean getBit(int bit) {
        return bitSet.get(bit);
    }

    /**
     * Set a single bit in the Bloom filter.
     *
     * @param bit   is the bit to set.
     * @param value If true, the bit is set. If false, the bit is cleared.
     */
    public void setBit(int bit, boolean value) {
        bitSet.set(bit, value);
    }

    /**
     * Return the bit set used to store the Bloom filter.
     *
     * @return bit set representing the Bloom filter.
     */
    public NeuralBitSet getBitSet() {
        return bitSet;
    }

    /**
     * Returns the number of bits in the Bloom filter. Use count() to retrieve
     * the number of inserted elements.
     *
     * @return the size of the bitSet used by the Bloom filter.
     */
    public int size() {
        return this.bitSetSize;
    }

    /**
     * Returns the number of elements added to the Bloom filter after it
     * was constructed or after clear() was called.
     *
     * @return number of elements added to the Bloom filter.
     */
    public int count() {
        return this.numberOfAddedElements;
    }

    /**
     * Returns is the bit set empty, bit set is empty means no any elements added to bloom filter.
     *
     * @return is the bit set empty
     */
    public boolean isEmpty() {
        return count() <= 0;
    }

    /**
     * Returns the expected number of elements to be inserted into the filter.
     * This value is the same value as the one passed to the constructor.
     *
     * @return expected number of elements.
     */
    public int getExpectedNumberOfElements() {
        return expectedNumberOfFilterElements;
    }

    /**
     * Get expected number of bits per element when the Bloom filter is full. This value is set by the constructor
     * when the Bloom filter is created. See also getBitsPerElement().
     *
     * @return expected number of bits per element.
     */
    public double getExpectedBitsPerElement() {
        return this.bitsPerElement;
    }

    /**
     * Get actual number of bits per element based on the number of elements that have currently been inserted and the length
     * of the Bloom filter. See also getExpectedBitsPerElement().
     *
     * @return number of bits per element.
     */
    public double getBitsPerElement() {
        return this.bitSetSize / (double) numberOfAddedElements;
    }

}