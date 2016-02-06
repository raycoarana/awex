package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.Collection;

class AfterAllPromise<Result, Progress> extends AwexPromise<MultipleResult<Result, Progress>, Progress> {

    private final Promise<Result, Progress>[] mPromises;
    private final Result[] mResults;
    private final Exception[] mErrors;

    private int mResolvedPromises = 0;

    @SuppressWarnings("unchecked")
    public AfterAllPromise(Awex awex, Collection<Promise<Result, Progress>> promises) {
        super(awex);

        mResults = (Result[]) new Object[promises.size()];
        mPromises = promises.toArray(new Promise[promises.size()]);
        mErrors = new Exception[promises.size()];

        int i = 0;
        for (Promise<Result, Progress> promise : mPromises) {
            final int promiseIndex = i;
            promise.done(new DoneCallback<Result>() {
                @Override
                public void onDone(Result result) {
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
                            cancelTask(false);
                        }
                    }
                }
            });
            i++;
        }
    }

    private MultipleResult<Result, Progress> buildResult() {
        return new MultipleResult<>(mPromises, mResults, mErrors);
    }

    @Override
    public void cancelTask(boolean mayInterrupt) {
        synchronized (this) {
            super.cancelTask(mayInterrupt);

            for (Promise<Result, Progress> promise : mPromises) {
                promise.cancelTask(mayInterrupt);
            }
        }
    }

}