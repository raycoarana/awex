package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class SingleThreadFilterPromise<Result, Progress> extends AbstractSingleThreadPromise<Result, Result, Progress> {

    public SingleThreadFilterPromise(Awex awex, CollectionPromise<Result, Progress> promise, final Filter<Result> filter) {
        super(awex, promise, new Apply<Result, Result>() {
            @Override
            public boolean shouldApply(Result item) {
                return filter.filter(item);
            }

            @Override
            public Result apply(Result item) {
                return item;
            }
        });
    }
}