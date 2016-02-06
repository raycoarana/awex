package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class SingleThreadFilterPromise<Result, Progress> extends AbstractSingleThreadPromise<Result, Result, Progress> {

    public SingleThreadFilterPromise(Awex awex, CollectionPromise<Result, Progress> promise, final Filter<Result> filter) {
        super(awex, promise, new Apply<Result, Result>() {
            @Override
            public Result apply(Result item) {
                return filter.filter(item) ? item : null;
            }
        });
    }
}