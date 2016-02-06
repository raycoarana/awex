package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.Arrays;
import java.util.Collection;

class AllOfPromise<T, P> extends AwexPromise<Collection<T>, P> {

    private final Collection<Promise<T, P>> mPromises;
    private final T[] mResults;

    private int mResolvedPromises = 0;

    @SuppressWarnings("unchecked")
    public AllOfPromise(Awex awex, Collection<Promise<T, P>> promises) {
        super(awex);

        mResults = (T[]) new Object[promises.size()];
        mPromises = promises;

        FailCallback mFailCallback = buildFailCallback();
        CancelCallback mCancelCallback = buildCancelCallback();

        int i = 0;
        for (Promise<T, P> promise : mPromises) {
            final int promiseIndex = i;
            promise.done(new DoneCallback<T>() {
                @Override
                public void onDone(T result) {
                    synchronized (AllOfPromise.this) {
                        if (getState() == STATE_PENDING) {
                            mResults[promiseIndex] = result;
                            mResolvedPromises++;
                            if (mResolvedPromises == mPromises.size()) {
                                resolve(Arrays.asList(mResults));
                            }
                        }
                    }
                }
            }).fail(mFailCallback).cancel(mCancelCallback);
            i++;
        }
    }

    private FailCallback buildFailCallback() {
        return new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                synchronized (AllOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        reject(exception);
                    }
                }
            }
        };
    }

    private CancelCallback buildCancelCallback() {
        return new CancelCallback() {
            @Override
            public void onCancel() {
                synchronized (AllOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        cancelTask(false);
                    }
                }
            }
        };
    }

    @Override
    public void cancelTask(boolean mayInterrupt) {
        synchronized (this) {
            super.cancelTask(mayInterrupt);

            for (Promise<T, P> promise : mPromises) {
                promise.cancelTask(mayInterrupt);
            }
        }
    }

}