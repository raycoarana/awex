package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class MapperTransformerPromise<T, U> extends AbstractTransformerPromise<T, U> {

    public MapperTransformerPromise(Awex awex, Promise<T> promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }

}
