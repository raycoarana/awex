package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class MultiThreadFilterPromise<T> extends AbstractMultiThreadPromise<T, T> {

    @SuppressWarnings("unchecked")
    public MultiThreadFilterPromise(Awex awex, Promise promise, final Filter<T> filter) {
        super(awex, promise, new Apply<T, T>() {
            @Override
            public T apply(T item) {
                return filter.filter(item) ? item : null;
            }
        });
    }

}
