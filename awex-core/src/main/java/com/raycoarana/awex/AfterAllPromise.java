package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.Collection;

class AfterAllPromise<T> extends AwexPromise<MultipleResult<T>> {

    private final Promise<T>[] mPromises;
    private final T[] mResults;
    private final Exception[] mErrors;

    private int mResolvedPromises = 0;

    @SuppressWarnings("unchecked")
    public AfterAllPromise(Awex awex, Collection<Promise<T>> promises) {
        super(awex);

        mResults = (T[]) new Object[promises.size()];
        mPromises = promises.toArray(new Promise[promises.size()]);
        mErrors = new Exception[promises.size()];

        int i = 0;
        for (Promise<T> promise : mPromises) {
            final int promiseIndex = i;
            promise.done(new DoneCallback<T>() {
                @Override
                public void onDone(T result) {
                    synchronized (AfterAllPromise.this) {
                        if (getState() == STATE_PENDING) {
                            mResults[promiseIndex] = result;
                            mResolvedPromises++;
                            if (mResolvedPromises == mPromises.length) {
                                resolve(buildResult());
                            }
                        }
                    }
                }
            }).fail(new FailCallback() {
                @Override
                public void onFail(Exception exception) {
                    synchronized (AfterAllPromise.this) {
                        if (getState() == STATE_PENDING) {
                            mErrors[promiseIndex] = exception;
                            mResolvedPromises++;
                            if (mResolvedPromises == mPromises.length) {
                                resolve(buildResult());
                            }
                        }
                    }
                }
            }).cancel(new CancelCallback() {
                @Override
                public void onCancel() {
                    synchronized (AfterAllPromise.this) {
                        if (getState() == STATE_PENDING) {
                            cancelWork(false);
                        }
                    }
                }
            });
            i++;
        }
    }

    private MultipleResult<T> buildResult() {
        return new MultipleResult<>(mPromises, mResults, mErrors);
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