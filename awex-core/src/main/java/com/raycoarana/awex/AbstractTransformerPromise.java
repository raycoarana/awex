package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.AbsentValueException;

abstract class AbstractTransformerPromise<T, U> extends AwexPromise<U> {

    protected final Apply<T, U> mApply;

    public AbstractTransformerPromise(Awex awex, Promise<T> promise, Apply<T, U> apply) {
        super(awex);

        mApply = apply;
        promise.done(new DoneCallback<T>() {
            @Override
            public void onDone(T result) {
                AbstractTransformerPromise.this.apply(result);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                AbstractTransformerPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                AbstractTransformerPromise.this.cancelTask();
            }
        });
    }

    private void apply(T item) {
        U result = mApply.apply(item);
        if (result != null) {
            resolve(result);
        } else {
            reject(new AbsentValueException());
        }
    }

}
