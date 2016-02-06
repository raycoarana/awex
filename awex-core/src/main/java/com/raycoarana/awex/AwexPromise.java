package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.AlwaysCallback;
import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.callbacks.UIAlwaysCallback;
import com.raycoarana.awex.callbacks.UICancelCallback;
import com.raycoarana.awex.callbacks.UIDoneCallback;
import com.raycoarana.awex.callbacks.UIFailCallback;
import com.raycoarana.awex.callbacks.UIProgressCallback;
import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of task promise
 */
class AwexPromise<Result, Progress> implements Promise<Result, Progress> {

    protected final Awex mAwex;

    private final Task mTask;
    private final UIThread mUIThread;
    private final Logger mLogger;
    private final long mId;

    private int mState;
    private Result mResult;
    private Exception mException;

    private final List<DoneCallback<Result>> mDoneCallbacks = new ArrayList<>();
    private final List<FailCallback> mFailCallbacks = new ArrayList<>();
    private final List<ProgressCallback<Progress>> mProgressCallbacks = new ArrayList<>();
    private final List<CancelCallback> mCancelCallbacks = new ArrayList<>();
    private final List<AlwaysCallback> mAlwaysCallbacks = new ArrayList<>();

    public AwexPromise(Awex awex) {
        this(awex, null);
    }

    public AwexPromise(Awex awex, Task task) {
        mAwex = awex;
        mTask = task;
        mId = mTask != null ? mTask.getId() : -1;
        mUIThread = awex.provideUIThread();
        mLogger = awex.provideLogger();
        mState = STATE_PENDING;
        printStateChanged("PENDING");
    }

    /**
     * Resolves the promise, triggers any done/always callbacks
     *
     * @param result value used to resolve the promise
     * @throws IllegalStateException if the promise is not in pending state
     */
    public Promise<Result, Progress> resolve(Result result) {
        synchronized (this) {
            validateInPendingState();

            mState = STATE_RESOLVED;
            printStateChanged("RESOLVED");
            mResult = result;
            if (mDoneCallbacks.size() > 0 || mAlwaysCallbacks.size() > 0) {
                mAwex.submit(new Runnable() {

                    @Override
                    public void run() {
                        triggerAllDones();
                        triggerAllAlways();
                        clearCallbacks();
                    }

                });
            } else {
                clearCallbacks();
            }
        }

        return this;
    }


    private void triggerAllDones() {
        synchronized (this) {
            for (final DoneCallback<Result> callback : mDoneCallbacks) {
                triggerDone(callback);
            }
        }
    }

    private void triggerDone(final DoneCallback<Result> callback) {
        if (callback instanceof UIDoneCallback && !mUIThread.isCurrentThread()) {
            mUIThread.post(new CancellableRunnable() {
                @Override
                public void execute() {
                    tryTrigger(callback, mResult);
                }
            });
        } else {
            tryTrigger(callback, mResult);
        }
    }

    private void tryTrigger(DoneCallback<Result> callback, Result result) {
        try {
            callback.onDone(result);
        } catch (Exception ex) {
            mLogger.e("Error when trigger done callback", ex);
        }
    }

    /**
     * Rejects the promise, triggers any fail/always callbacks
     *
     * @param ex exception that represents the rejection of the promise
     */
    public Promise<Result, Progress> reject(Exception ex) {
        synchronized (this) {
            if (mState == STATE_CANCELLED) {
                return this;
            }
            validateInPendingState();

            mState = STATE_REJECTED;
            printStateChanged("REJECTED");
            mException = ex;
            if (mFailCallbacks.size() > 0 || mAlwaysCallbacks.size() > 0) {
                mAwex.submit(new Runnable() {

                    @Override
                    public void run() {
                        triggerAllFails();
                        triggerAllAlways();
                        clearCallbacks();
                    }

                });
            } else {
                clearCallbacks();
            }
        }

        return this;
    }

    private void triggerAllFails() {
        synchronized (this) {
            for (final FailCallback callback : mFailCallbacks) {
                triggerFail(callback);
            }
        }
    }

    private void triggerFail(final FailCallback callback) {
        if (callback instanceof UIFailCallback && !mUIThread.isCurrentThread()) {
            mUIThread.post(new CancellableRunnable() {
                @Override
                public void execute() {
                    tryTrigger(callback, mException);
                }
            });
        } else {
            tryTrigger(callback, mException);
        }
    }

    private void tryTrigger(FailCallback callback, Exception exception) {
        try {
            callback.onFail(exception);
        } catch (Exception ex) {
            mLogger.e("Error when trigger fail callback", ex);
        }
    }

    private void triggerAllAlways() {
        synchronized (this) {
            for (final AlwaysCallback callback : mAlwaysCallbacks) {
                triggerAlways(callback);
            }
        }
    }

    private void triggerAlways(final AlwaysCallback callback) {
        if (callback instanceof UIAlwaysCallback && !mUIThread.isCurrentThread()) {
            mUIThread.post(new CancellableRunnable() {
                @Override
                public void execute() {
                    tryTrigger(callback);
                }
            });
        } else {
            tryTrigger(callback);
        }
    }

    private void tryTrigger(AlwaysCallback callback) {
        try {
            callback.onAlways();
        } catch (Exception ex) {
            mLogger.e("Error when trigger always callback", ex);
        }
    }

    /**
     * Notify progress to all callbacks
     * @param progress amount of progress
     */
    public void notifyProgress(Progress progress) {
        synchronized (this) {
            validateInPendingState();

            mLogger.v("Promise of task " + mId + " progress to " + progress);

            if (mProgressCallbacks.size() > 0) {
                triggerAllProgress(progress);
            }
        }
    }

    private void triggerAllProgress(Progress progress) {
        for (final ProgressCallback<Progress> callback : mProgressCallbacks) {
            triggerProgress(callback, progress);
        }
    }

    private void triggerProgress(final ProgressCallback<Progress> callback, final Progress progress) {
        if (callback instanceof UIProgressCallback && !mUIThread.isCurrentThread()) {
            mUIThread.post(new CancellableRunnable() {
                @Override
                public void execute() {
                    tryTrigger(callback, progress);
                }
            });
        } else {
            tryTrigger(callback, progress);
        }
    }

    private void tryTrigger(ProgressCallback<Progress> callback, Progress progress) {
        try {
            callback.onProgress(progress);
        } catch (Exception ex) {
            mLogger.e("Error when trigger done callback", ex);
        }
    }

    private void validateInPendingState() {
        if (mState != STATE_PENDING) {
            throw new IllegalStateException("Illegal promise state for this operation");
        }
    }

    @Override
    public void cancelTask() {
        cancelTask(false);
    }

    @Override
    public void cancelTask(final boolean mayInterrupt) {
        synchronized (this) {
            if (mState == STATE_PENDING) {
                mState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
                if (mUIThread.isCurrentThread() && mCancelCallbacks.size() > 0) {
                    mAwex.submit(new Runnable() {

                        @Override
                        public void run() {
                            doCancel(mayInterrupt);
                        }

                    });
                } else {
                    doCancel(mayInterrupt);
                }
            }
        }
    }

    private void doCancel(boolean mayInterrupt) {
        synchronized (this) {
            if (mTask != null) {
                mAwex.cancel(mTask, mayInterrupt);
            }
            triggerAllCancel();
            clearCallbacks();
        }
    }

    private void triggerAllCancel() {
        for (final CancelCallback callback : mCancelCallbacks) {
            triggerCancel(callback);
        }
    }

    private void triggerCancel(final CancelCallback callback) {
        if (callback instanceof UICancelCallback && !mUIThread.isCurrentThread()) {
            mUIThread.post(new Runnable() {
                @Override
                public void run() {
                    tryTrigger(callback);
                }
            });
        } else {
            tryTrigger(callback);
        }
    }

    private void tryTrigger(CancelCallback callback) {
        try {
            callback.onCancel();
        } catch (Exception ex) {
            mLogger.e("Error when trigger always callback", ex);
        }
    }

    private void clearCallbacks() {
        mDoneCallbacks.clear();
        mFailCallbacks.clear();
        mProgressCallbacks.clear();
        mCancelCallbacks.clear();
        mAlwaysCallbacks.clear();

        notifyAll();
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isPending() {
        return getState() == STATE_PENDING;
    }

    @Override
    public boolean isResolved() {
        return getState() == STATE_RESOLVED;
    }

    @Override
    public boolean isRejected() {
        return getState() == STATE_REJECTED;
    }

    @Override
    public boolean isCancelled() {
        return getState() == STATE_CANCELLED;
    }

    @Override
    public boolean isCompleted() {
        int state = getState();
        return state == STATE_RESOLVED || state == STATE_REJECTED || state == STATE_CANCELLED;
    }

    @Override
    public Result getResult() throws Exception {
        synchronized (this) {
            blockWhilePending();

            switch (mState) {
                case STATE_CANCELLED:
                    throw new IllegalStateException("Couldn't get result from a cancelled promise");
                case STATE_REJECTED:
                    throw mException;
                default: //Promise.STATE_RESOLVED:
                    return mResult;
            }
        }
    }

    @Override
    public Result getResultOrDefault(Result defaultValue) throws InterruptedException {
        synchronized (this) {
            blockWhilePending();

            switch (mState) {
                case STATE_CANCELLED:
                case STATE_REJECTED:
                    return defaultValue;
                default: //Promise.STATE_RESOLVED:
                    return mResult;
            }
        }
    }

    private void blockWhilePending() throws InterruptedException {
        synchronized (this) {
            while (isPending()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    @Override
    public Promise<Result, Progress> done(final DoneCallback<Result> callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mDoneCallbacks.add(callback);
                    break;
                case STATE_RESOLVED:
                    if (shouldExecuteInBackground(callback)) {
                        mAwex.submit(new Runnable() {

                            @Override
                            public void run() {
                                tryTrigger(callback, mResult);
                            }

                        });
                    } else {
                        triggerDone(callback);
                    }
                    break;
            }
        }
        return this;
    }

    private boolean shouldExecuteInBackground(DoneCallback<Result> callback) {
        return mUIThread.isCurrentThread() && !(callback instanceof UIDoneCallback);
    }

    @Override
    public Promise<Result, Progress> fail(final FailCallback callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mFailCallbacks.add(callback);
                    break;
                case STATE_REJECTED:
                    if (shouldExecuteInBackground(callback)) {
                        mAwex.submit(new Runnable() {

                            @Override
                            public void run() {
                                tryTrigger(callback, mException);
                            }
                        });
                    } else {
                        triggerFail(callback);
                    }
                    break;
            }
        }
        return this;
    }

    private boolean shouldExecuteInBackground(FailCallback callback) {
        return mUIThread.isCurrentThread() && !(callback instanceof UIFailCallback);
    }

    @Override
    public Promise<Result, Progress> progress(final ProgressCallback<Progress> callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mProgressCallbacks.add(callback);
                    break;
            }
        }
        return this;
    }

    @Override
    public Promise<Result, Progress> cancel(final CancelCallback callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mCancelCallbacks.add(callback);
                    break;
                case STATE_CANCELLED:
                    if (shouldExecuteInBackground(callback)) {
                        mAwex.submit(new Runnable() {

                            @Override
                            public void run() {
                                tryTrigger(callback);
                            }
                        });
                    } else {
                        triggerCancel(callback);
                    }
                    break;
            }
        }
        return this;
    }

    private boolean shouldExecuteInBackground(CancelCallback callback) {
        return mUIThread.isCurrentThread() && !(callback instanceof UICancelCallback);
    }

    @Override
    public Promise<Result, Progress> always(final AlwaysCallback callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mAlwaysCallbacks.add(callback);
                    break;
                case STATE_RESOLVED:
                case STATE_REJECTED:
                    if (shouldExecuteInBackground(callback)) {
                        mAwex.submit(new Runnable() {

                            @Override
                            public void run() {
                                tryTrigger(callback);
                            }
                        });
                    } else {
                        triggerAlways(callback);
                    }
                    break;
            }
        }
        return this;
    }

    @Override
    public Promise<Result, Progress> or(Promise<Result, Progress> promise) {
        return new OrPromise<>(mAwex, this, promise);
    }

    @Override
    public Promise<Collection<Result>, Progress> and(Promise<Result, Progress> promise) {
        List<Promise<Result, Progress>> promises = Arrays.asList(this, promise);
        return new AllOfPromise<>(mAwex, promises);
    }

    private boolean shouldExecuteInBackground(AlwaysCallback callback) {
        return mUIThread.isCurrentThread() && !(callback instanceof UIAlwaysCallback);
    }

    private void printStateChanged(String newState) {
        mLogger.v("Promise of task " + mId + " changed to state " + newState);
    }

    @Override
    public <U> Promise<U, Progress> mapSingle(Mapper<Result, U> mapper) {
        return new MapperTransformerPromise<>(mAwex, this, mapper);
    }

    @Override
    public Promise<Result, Progress> filterSingle(Filter<Result> filter) {
        return new FilterTransformerPromise<>(mAwex, this, filter);
    }

    @Override
    public <R> CollectionPromise<R, Progress> stream() {
        return new AwexCollectionPromise<>(mAwex, this);
    }

    private abstract class CancellableRunnable implements Runnable {

        @Override
        public void run() {
            if (mState != STATE_CANCELLED) {
                execute();
            }
        }

        public abstract void execute();

    }
}
