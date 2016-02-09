package com.raycoarana.awex.util;

public interface Map<K, V> {
    int size();

    V get(Object key);

    boolean containsKey(Object key);

    V put(K key, V value);

    V remove(K key);

    Iterable<V> values();

    ArrayMap<K, V> clone();

    void clear();

    class Provider {
        public static <K, V> Map<K, V> get() {
            return new ArrayMap<>();
        }

        public static <K, V> Map<K, V> getSync() {
            return new SyncArrayMap<>();
        }
    }
}
