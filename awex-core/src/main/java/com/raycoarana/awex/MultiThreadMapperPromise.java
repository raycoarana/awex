package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class MultiThreadMapperPromise<T, U> extends AbstractMultiThreadPromise<T, U> {

    @SuppressWarnings("unchecked")
    public MultiThreadMapperPromise(Awex awex, Promise promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }

}
