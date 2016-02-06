package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.ArrayList;
import java.util.Collection;

abstract class AbstractSingleThreadPromise<T, U, P> extends AwexCollectionPromise<U, P> {

    protected final Apply<T, U> mApply;

    public AbstractSingleThreadPromise(Awex awex, CollectionPromise<T, P> promise, Apply<T, U> apply) {
        super(awex);

        mApply = apply;
        promise.done(new DoneCallback<Collection<T>>() {
            @Override
            public void onDone(Collection<T> result) {
                AbstractSingleThreadPromise.this.apply(result);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                AbstractSingleThreadPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                AbstractSingleThreadPromise.this.cancelTask();
            }
        });
    }

    protected void apply(Collection<T> items) {
        Collection<U> results = applyToCollection(items);
        resolve(results);
    }

    protected Collection<U> applyToCollection(Iterable<T> items) {
        Collection<U> results = new ArrayList<>();
        for (T item : items) {
            U result = mApply.apply(item);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

}
