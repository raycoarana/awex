package com.raycoarana.awex;

import com.raycoarana.awex.transform.Func;

public class SingleThreadForEachPromise<Result, Progress> extends AbstractSingleThreadPromise<Result, Result, Progress> {

    public SingleThreadForEachPromise(Awex awex, CollectionPromise<Result, Progress> promise, final Func<Result> func) {
        super(awex, promise, new Apply.ApplyAdapter<Result, Result>() {
            @Override
            public Result apply(Result item) {
                func.run(item);
                return item;
            }
        });
    }

}
