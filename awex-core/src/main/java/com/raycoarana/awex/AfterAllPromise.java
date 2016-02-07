package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class AfterAllPromise<Result, Progress> extends AwexPromise<MultipleResult<Result, Progress>, Progress> {

    private final Promise<Result, Progress>[] mPromises;
    private final Result[] mResults;
    private final Exception[] mErrors;
    private final ReentrantReadWriteLock mCancelLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock mCancelReadLock = mCancelLock.readLock();
    private final ReentrantReadWriteLock.WriteLock mCancelWriteLock = mCancelLock.writeLock();
    private AtomicInteger mResolvedPromises = new AtomicInteger(0);

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
                    try {
                        mCancelReadLock.lock();
                        if (getState() == STATE_PENDING) {
                            mResults[promiseIndex] = result;
                            int resolvedPromises = mResolvedPromises.incrementAndGet();
                            if (resolvedPromises == mPromises.length) {
                                resolve(buildResult());
                            }
                        }
                    } finally {
                        mCancelReadLock.unlock();
                    }
                }
            }).fail(new FailCallback() {
                @Override
                public void onFail(Exception exception) {
                    try {
                        mCancelReadLock.lock();
                        if (getState() == STATE_PENDING) {
                            mErrors[promiseIndex] = exception;
                            int resolvedPromises = mResolvedPromises.incrementAndGet();
                            if (resolvedPromises == mPromises.length) {
                                resolve(buildResult());
                            }
                        }
                    } finally {
                        mCancelReadLock.unlock();
                    }
                }
            }).cancel(mCancelCallback);
            i++;
        }
        if (i == 0) {
            resolve(buildResult());
        }
    }

    private final CancelCallback mCancelCallback = new CancelCallback() {
        @Override
        public void onCancel() {
            try {
                mCancelWriteLock.lock();
                if (getState() == STATE_PENDING) {
                    cancelTask(false);
                }
            } finally {
                mCancelWriteLock.unlock();
            }
        }
    };

    private MultipleResult<Result, Progress> buildResult() {
        return new MultipleResult<>(mPromises, mResults, mErrors);
    }

    @Override
    public void cancelTask(boolean mayInterrupt) {
        try {
            mCancelWriteLock.lock();
            super.cancelTask(mayInterrupt);

            for (Promise<Result, Progress> promise : mPromises) {
                promise.cancelTask(mayInterrupt);
            }
        } finally {
            mCancelWriteLock.unlock();
        }
    }

}