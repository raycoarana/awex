package com.raycoarana.awex.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SyncArrayMap<K, V> extends ArrayMap<K, V> {

    private ReentrantReadWriteLock mReentrantReadWriteLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock mReadLock = mReentrantReadWriteLock.readLock();
    private ReentrantReadWriteLock.WriteLock mWriteLock = mReentrantReadWriteLock.writeLock();

    @Override
    public int size() {
        try {
            mReadLock.lock();
            return super.size();
        } finally {
            mReadLock.unlock();
        }
    }

    @Override
    public V get(Object key) {
        try {
            mReadLock.lock();
            return super.get(key);
        } finally {
            mReadLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            mReadLock.lock();
            return super.containsKey(key);
        } finally {
            mReadLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        try {
            mWriteLock.lock();
            return super.put(key, value);
        } finally {
            mWriteLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        try {
            mWriteLock.lock();
            return super.remove(key);
        } finally {
            mWriteLock.unlock();
        }
    }

    @Override
    public Iterable<V> values() {
        throw new UnsupportedOperationException("Clone this object and then call values().");
    }

    @Override
    public void clear() {
        try {
            mWriteLock.lock();
            super.clear();
        } finally {
            mWriteLock.unlock();
        }
    }

    @Override
    public ArrayMap<K, V> clone() {
        try {
            mReadLock.lock();
            return super.clone();
        } finally {
            mReadLock.unlock();
        }
    }

}
