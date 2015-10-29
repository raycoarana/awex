package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

abstract class AbstractSingleThreadPromise<T, U> extends AwexPromise<Collection<U>> {

    protected final Apply<T, U> mApply;

    public interface Apply<T, U> {
        U apply(T item);
    }

    @SuppressWarnings("unchecked")
    public AbstractSingleThreadPromise(Awex awex, Promise promise, Apply<T, U> apply) {
        super(awex);

        mApply = apply;
        promise.done(new DoneCallback() {
            @Override
            public void onDone(Object result) {
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
                AbstractSingleThreadPromise.this.cancelWork();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void apply(Object item) {
        if (item instanceof Iterable) {
            applyToCollectionAndResolve((Iterable<T>) item);
        } else {
            applyToSingleAndResolve((T) item);
        }
    }

    protected void applyToCollectionAndResolve(Iterable<T> items) {
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

    private void applyToSingleAndResolve(T item) {
        Collection<U> results;
        U result = mApply.apply(item);
        if (result != null) {
            results = Collections.singletonList(result);
        } else {
            results = Collections.emptyList();
        }
        resolve(results);
    }

}
