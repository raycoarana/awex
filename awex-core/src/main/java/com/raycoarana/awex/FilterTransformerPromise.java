package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class FilterTransformerPromise<T> extends AbstractTransformerPromise<T, T> {

    public FilterTransformerPromise(Awex awex, Promise<T> promise, final Filter<T> filter) {
        super(awex, promise, new Apply<T, T>() {
            @Override
            public T apply(T item) {
                return filter.filter(item) ? item : null;
            }
        });
    }

}
