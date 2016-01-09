package com.raycoarana.awex.transform;

public interface Reducer<T> {
    T reduce(T v1, T v2);
}
