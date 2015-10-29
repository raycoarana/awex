package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class SingleThreadMapperPromise<T, U> extends AbstractSingleThreadPromise<T, U> {

    public SingleThreadMapperPromise(Awex awex, Promise promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }
}