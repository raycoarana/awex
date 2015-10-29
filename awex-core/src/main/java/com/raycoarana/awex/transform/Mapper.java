package com.raycoarana.awex.transform;

public interface Mapper<T, U> {
    U map(T value);
}
