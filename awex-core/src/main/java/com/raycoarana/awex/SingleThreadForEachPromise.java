package com.raycoarana.awex;

import com.raycoarana.awex.transform.Func;

public class SingleThreadForEachPromise<T> extends AbstractSingleThreadPromise<T, T> {

    public SingleThreadForEachPromise(Awex awex, CollectionPromise<T> promise, final Func<T> func) {
        super(awex, promise, new Apply<T, T>() {
            @Override
            public T apply(T item) {
                func.run(item);
                return item;
            }
        });
    }

}
