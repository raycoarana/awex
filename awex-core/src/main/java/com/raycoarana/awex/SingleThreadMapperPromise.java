package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class SingleThreadMapperPromise<T, U, Progress> extends AbstractSingleThreadPromise<T, U, Progress> {

    public SingleThreadMapperPromise(Awex awex, CollectionPromise<T, Progress> promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply.ApplyAdapter<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }
}