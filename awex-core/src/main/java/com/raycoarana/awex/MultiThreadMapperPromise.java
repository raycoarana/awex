package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class MultiThreadMapperPromise<T, U, Progress> extends AbstractMultiThreadPromise<T, U, Progress> {

    public MultiThreadMapperPromise(Awex awex, CollectionPromise<T, Progress> promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply.ApplyAdapter<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }

}
