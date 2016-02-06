package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class MultiThreadFilterPromise<Result, Progress> extends AbstractMultiThreadPromise<Result, Result, Progress> {

    public MultiThreadFilterPromise(Awex awex, CollectionPromise<Result, Progress> promise, final Filter<Result> filter) {
        super(awex, promise, new Apply<Result, Result>() {
            @Override
            public Result apply(Result item) {
                return filter.filter(item) ? item : null;
            }
        });
    }

}
