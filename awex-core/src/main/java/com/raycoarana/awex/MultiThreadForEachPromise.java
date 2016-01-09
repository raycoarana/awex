package com.raycoarana.awex;

import com.raycoarana.awex.transform.Func;

public class MultiThreadForEachPromise<T> extends AbstractMultiThreadPromise<T, T> {

    public MultiThreadForEachPromise(Awex awex, CollectionPromise<T> promise, final Func<T> func) {
        super(awex, promise, new Apply<T, T>() {
            @Override
            public T apply(T item) {
                func.run(item);
                return item;
            }
        });
    }

}
