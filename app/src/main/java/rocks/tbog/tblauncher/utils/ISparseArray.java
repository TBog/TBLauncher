package rocks.tbog.tblauncher.utils;

public interface ISparseArray<E> {
    /**
     * Returns true if the key exists in the array. This is equivalent to
     * {@link #indexOfKey(int)} >= 0.
     *
     * @param key Potential key in the mapping
     * @return true if the key is defined in the mapping
     */
    boolean contains(int key);

    /**
     * Gets the Object mapped from the specified key, or <code>null</code>
     * if no such mapping has been made.
     */
    E get(int key);

    /**
     * Gets the Object mapped from the specified key, or the specified Object
     * if no such mapping has been made.
     */
    E get(int key, E valueIfKeyNotFound);

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    void delete(int key);

    /**
     * Alias for {@link #delete(int)}.
     */
    default void remove(int key) {
        delete(key);
    }

    /**
     * Removes the mapping at the specified index.
     */
    void removeAt(int index);

    /**
     * Remove a range of mappings as a batch.
     *
     * @param index Index to begin at
     * @param size  Number of mappings to remove
     *
     *              <p>For indices outside of the range <code>0...size()-1</code>,
     *              the behavior is undefined.</p>
     */
    void removeAtRange(int index, int size);

    /**
     * Alias for {@link #put(int, Object)} to support Kotlin [index]= operator.
     *
     * @see #put(int, Object)
     */
    default void set(int key, E value) {
        put(key, value);
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    void put(int key, E value);

    /**
     * Returns the number of key-value mappings that this SparseArray
     * currently stores.
     */
    int size();

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the key from the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     *
     * <p>The keys corresponding to indices in ascending order are guaranteed to
     * be in ascending order, e.g., <code>keyAt(0)</code> will return the
     * smallest key and <code>keyAt(size()-1)</code> will return the largest
     * key.</p>
     */
    int keyAt(int index);

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the value from the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     *
     * <p>The values corresponding to indices in ascending order are guaranteed
     * to be associated with keys in ascending order, e.g.,
     * <code>valueAt(0)</code> will return the value associated with the
     * smallest key and <code>valueAt(size()-1)</code> will return the value
     * associated with the largest key.</p>
     */
    E valueAt(int index);

    /**
     * Given an index in the range <code>0...size()-1</code>, sets a new
     * value for the <code>index</code>th key-value mapping that this
     * SparseArray stores.
     */
    void setValueAt(int index, E value);

    /**
     * Returns the index for which {@link #keyAt} would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    int indexOfKey(int key);

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified value, or a negative number if no keys map to the
     * specified value.
     * <p>Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     * <p>Note also that unlike most collections' {@code indexOf} methods,
     * this method compares values using {@code ==} rather than {@code equals}.
     */
    int indexOfValue(E value);

    /**
     * Removes all key-value mappings from this SparseArray.
     */
    void clear();

    /**
     * Puts a key/value pair into the array, optimizing for the case where
     * the key is greater than all existing keys in the array.
     */
    void append(int key, E value);
}
