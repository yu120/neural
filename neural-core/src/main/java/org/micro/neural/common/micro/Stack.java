package org.micro.neural.common.micro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/**
 * The Stack
 *
 * @author lry
 */
public class Stack<E> implements Serializable {

    private int mSize = 0;

    private List<E> mElements = new ArrayList<E>();

    /**
     * push.
     *
     * @param ele {@link E}
     */
    public void push(E ele) {
        if (mElements.size() > mSize) {
            mElements.set(mSize, ele);
        } else {
            mElements.add(ele);
        }
        mSize++;
    }

    /**
     * pop.
     *
     * @return the last element.
     */
    public E pop() {
        if (mSize == 0) {
            throw new EmptyStackException();
        }

        return mElements.set(--mSize, null);
    }

    /**
     * peek.
     *
     * @return the last element.
     */
    public E peek() {
        if (mSize == 0) {
            throw new EmptyStackException();
        }

        return mElements.get(mSize - 1);
    }

    /**
     * get.
     *
     * @param index index.
     * @return element.
     */
    public E get(int index) {
        if (index >= mSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }

        return index < 0 ? mElements.get(index + mSize) : mElements.get(index);
    }

    /**
     * set.
     *
     * @param index index.
     * @param value element.
     * @return old element.
     */
    public E set(int index, E value) {
        if (index >= mSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }

        return mElements.set(index < 0 ? index + mSize : index, value);
    }

    /**
     * remove.
     *
     * @param index list index
     * @return element
     */
    public E remove(int index) {
        if (index >= mSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }

        E ret = mElements.remove(index < 0 ? index + mSize : index);
        mSize--;
        return ret;
    }

    /**
     * get stack size.
     *
     * @return size.
     */
    public int size() {
        return mSize;
    }

    /**
     * is empty.
     *
     * @return empty or not.
     */
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * clear stack.
     */
    public void clear() {
        mSize = 0;
        mElements.clear();
    }

}