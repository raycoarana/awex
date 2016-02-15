package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

public class FilterTransformerPromise<Result, Progress> extends AbstractTransformerPromise<Result, Result, Progress> {

    public FilterTransformerPromise(Awex awex, Promise<Result, Progress> promise, final Filter<Result> filter) {
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
