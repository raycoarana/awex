package com.raycoarana.awex;

public interface Apply<T, U> {
    U apply(T item);
}
