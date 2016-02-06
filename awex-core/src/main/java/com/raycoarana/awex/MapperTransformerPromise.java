package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

public class MapperTransformerPromise<T, U, P> extends AbstractTransformerPromise<T, U, P> {

    public MapperTransformerPromise(Awex awex, Promise<T, P> promise, final Mapper<T, U> mapper) {
        super(awex, promise, new Apply<T, U>() {
            @Override
            public U apply(T item) {
                return mapper.map(item);
            }
        });
    }

}
