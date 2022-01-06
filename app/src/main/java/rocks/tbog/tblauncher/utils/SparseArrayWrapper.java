/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.tbog.tblauncher.utils;

import android.os.Build;
import android.util.SparseArray;

public class SparseArrayWrapper<E> implements ISparseArray<E> {
    protected SparseArray<E> mArray;
    protected int mCapacity;

    /**
     * Creates a new SparseArray containing no mappings.
     */
    public SparseArrayWrapper() {
        this(0);
    }

    /**
     * Creates a new SparseArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.
     */
    public SparseArrayWrapper(int initialCapacity) {
        mCapacity = initialCapacity;
        mArray = new SparseArray<>(mCapacity);
    }

    /**
     * Increases the capacity of this ArrayList instance, if necessary,
     * to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     * Should only be used with an empty array because the copy
     * operation is not optimized
     *
     * @param capacity the desired minimum capacity
     */
    public void ensureCapacity(int capacity) {
        if (mCapacity < capacity) {
            SparseArray<E> oldArray = mArray;
            int size = oldArray.size();

            mCapacity = capacity;
            mArray = new SparseArray<>(mCapacity);
            for (int index = 0; index < size; index += 1) {
                int key = oldArray.keyAt(index);
                E value = oldArray.valueAt(index);
                mArray.append(key, value);
            }
        }
    }

    @Override
    public boolean contains(int key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return mArray.contains(key);
        }
        return mArray.indexOfKey(key) >= 0;
    }

    @Override
    public E get(int key) {
        return mArray.get(key);
    }

    @Override
    public E get(int key, E valueIfKeyNotFound) {
        return mArray.get(key, valueIfKeyNotFound);
    }

    @Override
    public void delete(int key) {
        mArray.delete(key);
    }

    @Override
    public void removeAt(int index) {
        mArray.removeAt(index);
    }

    @Override
    public void removeAtRange(int index, int size) {
        mArray.removeAtRange(index, size);
    }

    @Override
    public void put(int key, E value) {
        mArray.put(key, value);
        updateCapacity();
    }

    @Override
    public int size() {
        return mArray.size();
    }

    public int capacity() {
        return mCapacity;
    }

    @Override
    public int keyAt(int index) {
        return mArray.keyAt(index);
    }

    @Override
    public E valueAt(int index) {
        return mArray.valueAt(index);
    }

    @Override
    public void setValueAt(int index, E value) {
        mArray.setValueAt(index, value);
    }

    @Override
    public int indexOfKey(int key) {
        return mArray.indexOfKey(key);
    }

    @Override
    public int indexOfValue(E value) {
        return mArray.indexOfValue(value);
    }

    @Override
    public void clear() {
        mArray.clear();
    }

    @Override
    public void append(int key, E value) {
        mArray.append(key, value);
        updateCapacity();
    }

    protected void updateCapacity() {
        int size = mArray.size();
        if (size > mCapacity)
            mCapacity = size;
    }
}