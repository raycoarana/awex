package com.raycoarana.awex.transform;

public interface Filter<T> {
    boolean filter(T value);
}
