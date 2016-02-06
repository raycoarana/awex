package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Func;
import com.raycoarana.awex.transform.Mapper;

import java.util.Collection;
import java.util.Collections;

class AwexCollectionPromise<Result, Progress> extends AwexPromise<Collection<Result>, Progress> implements CollectionPromise<Result, Progress> {

    public AwexCollectionPromise(Awex awex) {
        super(awex);
    }

    public <U> AwexCollectionPromise(Awex mAwex, Promise<U, Progress> promise) {
        super(mAwex);

        promise.done(new DoneCallback<U>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onDone(U result) {
                Collection<Result> collectionResult;
                if (result instanceof Collection) {
                    collectionResult = (Collection<Result>) result;
                } else {
                    collectionResult = (Collection<Result>) Collections.singleton(result);
                }
                AwexCollectionPromise.this.resolve(collectionResult);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                AwexCollectionPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                AwexCollectionPromise.this.cancelTask();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public CollectionPromise<Result, Progress> stream() {
        return this;
    }

    @Override
    public CollectionPromise<Result, Progress> filter(Filter<Result> filter) {
        return new SingleThreadFilterPromise<>(mAwex, this, filter);
    }

    @Override
    public CollectionPromise<Result, Progress> filterParallel(Filter<Result> filter) {
        if (mAwex.getNumberOfThreads() > 1) {
            return new MultiThreadFilterPromise<>(mAwex, this, filter);
        } else {
            return filter(filter);
        }
    }

    @Override
    public <U> CollectionPromise<U, Progress> map(Mapper<Result, U> mapper) {
        return new SingleThreadMapperPromise<>(mAwex, this, mapper);
    }

    @Override
    public <U> CollectionPromise<U, Progress> mapParallel(Mapper<Result, U> mapper) {
        if (mAwex.getNumberOfThreads() > 1) {
            return new MultiThreadMapperPromise<>(mAwex, this, mapper);
        } else {
            return map(mapper);
        }
    }

    @Override
    public CollectionPromise<Result, Progress> forEach(Func<Result> func) {
        return new SingleThreadForEachPromise<>(mAwex, this, func);
    }

    @Override
    public CollectionPromise<Result, Progress> forEachParallel(Func<Result> func) {
        if (mAwex.getNumberOfThreads() > 1) {
            return new MultiThreadForEachPromise<>(mAwex, this, func);
        } else {
            return forEach(func);
        }
    }

    @Override
    public Promise<Result, Progress> singleOrFirst() {
        return new MapperTransformerPromise<>(mAwex, this, new Mapper<Collection<Result>, Result>() {
            @Override
            public Result map(Collection<Result> value) {
                return value.size() > 0 ? value.iterator().next() : null;
            }
        });
    }

}
