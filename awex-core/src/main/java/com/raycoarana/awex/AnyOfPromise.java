package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.AllFailException;

import java.util.Collection;

class AnyOfPromise<T> extends AwexPromise<T> {

    private final Collection<Promise<T>> mPromises;
    private final Exception[] mExceptions;

    private int mFailedPromises = 0;

    @SuppressWarnings("unchecked")
    public AnyOfPromise(Awex awex, Collection<Promise<T>> promises) {
        super(awex);

        mExceptions = new Exception[promises.size()];
        mPromises = promises;

        DoneCallback<T> mDoneCallback = buildDoneCallback();
        CancelCallback mCancelCallback = buildCancelCallback();

        int i = 0;
        for (Promise<T> promise : mPromises) {
            final int promiseIndex = i;
            promise.done(mDoneCallback).fail(new FailCallback() {
                @Override
                public void onFail(Exception ex) {
                    synchronized (AnyOfPromise.this) {
                        if (getState() == STATE_PENDING) {
                            mExceptions[promiseIndex] = ex;
                            mFailedPromises++;
                            if (mFailedPromises == mPromises.size()) {
                                reject(new AllFailException(mExceptions));
                            }
                        }
                    }
                }
            }).cancel(mCancelCallback);

            i++;
        }
    }

    private DoneCallback<T> buildDoneCallback() {
        return new DoneCallback<T>() {
            @Override
            public void onDone(T result) {
                synchronized (AnyOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        resolve(result);
                    }
                }
            }
        };
    }

    private CancelCallback buildCancelCallback() {
        return new CancelCallback() {
            @Override
            public void onCancel() {
                synchronized (AnyOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        cancelWork(false);
                    }
                }
            }
        };
    }

    @Override
    public void cancelWork(boolean mayInterrupt) {
        synchronized (this) {
            super.cancelWork(mayInterrupt);

            for (Promise<T> promise : mPromises) {
                promise.cancelWork(mayInterrupt);
            }
        }
    }

}