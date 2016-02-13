package com.raycoarana.awex.util;

public class ObjectPool<T> {
    private final Object mLock = new Object();
    private final Object[] mPool;

    private int mPoolSize;

    /**
     * Creates a new instance.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    public ObjectPool(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("The max pool size must be > 0");
        }
        mPool = new Object[maxPoolSize];
    }

    @SuppressWarnings("unchecked")
    public T acquire() {
        synchronized (mLock) {
            if (mPoolSize > 0) {
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) mPool[lastPooledIndex];
                mPool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }
    }

    public boolean release(T element) {
        synchronized (mLock) {
            if (isInPool(element)) {
                throw new IllegalStateException("Already in the pool!");
            }
            if (mPoolSize < mPool.length) {
                mPool[mPoolSize] = element;
                mPoolSize++;
                return true;
            }
            return false;
        }
    }

    private boolean isInPool(T element) {
        for (int i = 0; i < mPoolSize; i++) {
            if (mPool[i] == element) {
                return true;
            }
        }
        return false;
    }
}
