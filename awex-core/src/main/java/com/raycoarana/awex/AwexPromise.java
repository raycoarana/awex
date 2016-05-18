package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.AlwaysCallback;
import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.callbacks.ThenCallback;
import com.raycoarana.awex.callbacks.UIAlwaysCallback;
import com.raycoarana.awex.callbacks.UICancelCallback;
import com.raycoarana.awex.callbacks.UIDoneCallback;
import com.raycoarana.awex.callbacks.UIFailCallback;
import com.raycoarana.awex.callbacks.UIProgressCallback;
import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Mapper;
import com.raycoarana.awex.util.ObjectPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of task promise
 */
class AwexPromise<Result, Progress> implements ResolvablePromise<Result, Progress> {

    protected final Awex mAwex;
    protected final Task mTask;

    private final ThreadHelper mThreadHelper;
    private final Logger mLogger;
    private final long mId;

    private int mState;
    private Result mResult;
    private Exception mException;
    private Callbacks<Result, Progress> mCallbacks;

    private final Object mProgressInOrderSyncObject = new Object();
    private final Object mBlockingObject = new Object();

    private static class Callbacks<Result, Progress> {
        public static final Callbacks EMPTY = new Callbacks(true);

        private final List<DoneCallback<Result>> mDoneCallbacks;
        private final List<FailCallback> mFailCallbacks;
        private final List<ProgressCallback<Progress>> mProgressCallbacks;
        private final List<CancelCallback> mCancelCallbacks;
        private final List<AlwaysCallback> mAlwaysCallbacks;

        private static final ObjectPool<Callbacks> sObjectPool = new ObjectPool<>(30);

        @SuppressWarnings("unchecked")
        public static <R, P> Callbacks<R, P> get() {
            Callbacks<R, P> callbacks = (Callbacks<R, P>) sObjectPool.acquire();
            if (callbacks == null) {
                callbacks = new Callbacks<>(false);
            }
            return callbacks;
        }

        private Callbacks(boolean empty) {
            if (empty) {
                mDoneCallbacks = Collections.emptyList();
                mFailCallbacks = Collections.emptyList();
                mProgressCallbacks = Collections.emptyList();
                mCancelCallbacks = Collections.emptyList();
                mAlwaysCallbacks = Collections.emptyList();
            } else {
                mDoneCallbacks = new ArrayList<>();
                mFailCallbacks = new ArrayList<>();
                mProgressCallbacks = new ArrayList<>();
                mCancelCallbacks = new ArrayList<>();
                mAlwaysCallbacks = new ArrayList<>();
            }
        }

        public List<DoneCallback<Result>> cloneDoneCallbacks() {
            return clone(mDoneCallbacks);
        }

        public List<FailCallback> cloneFailCallbacks() {
            return clone(mFailCallbacks);
        }

        public List<ProgressCallback<Progress>> cloneProgressCallbacks() {
            return clone(mProgressCallbacks);
        }

        public List<AlwaysCallback> cloneAlwaysCallbacks() {
            return clone(mAlwaysCallbacks);
        }

        public List<CancelCallback> cloneCancelCallbacks() {
            return clone(mCancelCallbacks);
        }

        @SuppressWarnings("unchecked")
        private <T> List<T> clone(List<T> items) {
            return items.size() == 0 ? Collections.<T>emptyList() : (List<T>) ((ArrayList) items).clone();
        }

        public void recycle() {
            if (this.equals(EMPTY)) {
                return;
            }

            mDoneCallbacks.clear();
            mFailCallbacks.clear();
            mProgressCallbacks.clear();
            mCancelCallbacks.clear();
            mAlwaysCallbacks.clear();

            sObjectPool.release(this);
        }
    }

    public AwexPromise(Awex awex) {
        this(awex, null);
    }

    public AwexPromise(Awex awex, Task task) {
        mAwex = awex;
        mTask = task;
        mId = mTask != null ? mTask.getId() : -1;
        mThreadHelper = awex.provideUIThread();
        mLogger = awex.provideLogger();
        mState = STATE_PENDING;
        mCallbacks = Callbacks.get();
        printStateChanged("PENDING");
    }

    /**
     * Resolves the promise, triggers any done/always callbacks
     *
     * @param result value used to resolve the promise
     * @throws IllegalStateException if the promise is not in pending state
     * @return this promise
     */
    @SuppressWarnings("unchecked")
    public Promise<Result, Progress> resolve(Result result) {
        List<DoneCallback<Result>> doneCallbacks;
        List<AlwaysCallback> alwaysCallbacks;
        synchronized (this) {
            validateInPendingState();

            mState = STATE_RESOLVED;
            printStateChanged("RESOLVED");
            mResult = result;

            doneCallbacks = mCallbacks.cloneDoneCallbacks();
            alwaysCallbacks = mCallbacks.cloneAlwaysCallbacks();
            clearCallbacks();
        }

        if (doneCallbacks.size() > 0 || alwaysCallbacks.size() > 0) {
            triggerAllDones(doneCallbacks);
            triggerAllAlways(alwaysCallbacks);
        }

        return this;
    }


    private void triggerAllDones(Collection<DoneCallback<Result>> doneCallbacks) {
        for (final DoneCallback<Result> callback : doneCallbacks) {
            triggerDone(callback);
        }
    }

    private void triggerDone(final DoneCallback<Result> callback) {
        if (callback instanceof UIDoneCallback && !mThreadHelper.isCurrentThread()) {
            mThreadHelper.post(new CancellableRunnable() {
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
     * @return this promise
     */
    @SuppressWarnings("unchecked")
    public Promise<Result, Progress> reject(Exception ex) {
        List<FailCallback> failCallbacks;
        List<AlwaysCallback> alwaysCallbacks;
        synchronized (this) {
            validateInPendingState();

            mState = STATE_REJECTED;
            printStateChanged("REJECTED");
            mException = ex;

            failCallbacks = mCallbacks.cloneFailCallbacks();
            alwaysCallbacks = mCallbacks.cloneAlwaysCallbacks();
            clearCallbacks();
        }

        if (failCallbacks.size() > 0 || alwaysCallbacks.size() > 0) {
            triggerAllFails(failCallbacks);
            triggerAllAlways(alwaysCallbacks);
        }

        return this;
    }

    private void triggerAllFails(Collection<FailCallback> failCallbacks) {
        for (final FailCallback callback : failCallbacks) {
            triggerFail(callback);
        }
    }

    private void triggerFail(final FailCallback callback) {
        if (callback instanceof UIFailCallback && !mThreadHelper.isCurrentThread()) {
            mThreadHelper.post(new CancellableRunnable() {
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

    private void triggerAllAlways(List<AlwaysCallback> alwaysCallbacks) {
        for (final AlwaysCallback callback : alwaysCallbacks) {
            triggerAlways(callback);
        }
    }

    private void triggerAlways(final AlwaysCallback callback) {
        if (callback instanceof UIAlwaysCallback && !mThreadHelper.isCurrentThread()) {
            mThreadHelper.post(new CancellableRunnable() {
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
     *
     * @param progress amount of progress
     */
    @SuppressWarnings("unchecked")
    public void notifyProgress(Progress progress) {
        List<ProgressCallback<Progress>> progressCallbacks;
        synchronized (this) {
            validateInPendingState();

            if (mLogger.isEnabled()) {
                mLogger.v("Promise of task " + mId + " progress to " + progress);
            }

            progressCallbacks = mCallbacks.cloneProgressCallbacks();
        }
        synchronized (mProgressInOrderSyncObject) {
            if (progressCallbacks.size() > 0) {
                triggerAllProgress(progress, progressCallbacks);
            }
        }
    }

    private void triggerAllProgress(Progress progress, Collection<ProgressCallback<Progress>> progressCallbacks) {
        for (final ProgressCallback<Progress> callback : progressCallbacks) {
            triggerProgress(callback, progress);
        }
    }

    private void triggerProgress(final ProgressCallback<Progress> callback, final Progress progress) {
        if (callback instanceof UIProgressCallback && !mThreadHelper.isCurrentThread()) {
            mThreadHelper.post(new CancellableRunnable() {
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

    @SuppressWarnings("unchecked")
    @Override
    public void cancelTask(final boolean mayInterrupt) {
        final int state;
        final List<CancelCallback> cancelCallbacks;
        synchronized (this) {
            state = mState;
            if (state == STATE_PENDING) {
                mState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
            }
            cancelCallbacks = mCallbacks.cloneCancelCallbacks();
            clearCallbacks();
        }
        if (state == STATE_PENDING) {
            if (mThreadHelper.isCurrentThread() && cancelCallbacks.size() > 0) {
                mAwex.submit(new Runnable() {

                    @Override
                    public void run() {
                        doCancel(mayInterrupt, cancelCallbacks);
                    }

                });
            } else {
                doCancel(mayInterrupt, cancelCallbacks);
            }
        }
    }

    private void doCancel(boolean mayInterrupt, Collection<CancelCallback> cancelCallbacks) {
        if (mTask != null) {
            mAwex.cancel(mTask, mayInterrupt);
        }
        triggerAllCancel(cancelCallbacks);
    }

    private void triggerAllCancel(Collection<CancelCallback> cancelCallbacks) {
        for (final CancelCallback callback : cancelCallbacks) {
            triggerCancel(callback);
        }
    }

    private void triggerCancel(final CancelCallback callback) {
        if (callback instanceof UICancelCallback && !mThreadHelper.isCurrentThread()) {
            mThreadHelper.post(new Runnable() {
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

    @SuppressWarnings("unchecked")
    private void clearCallbacks() {
        mCallbacks.recycle();
        mCallbacks = Callbacks.EMPTY;

        synchronized (mBlockingObject) {
            mBlockingObject.notifyAll();
        }
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

    @Override
    public Result getResultOrDefault(Result defaultValue) throws InterruptedException {
        blockWhilePending();

        switch (mState) {
            case STATE_CANCELLED:
            case STATE_REJECTED:
                return defaultValue;
            default: //Promise.STATE_RESOLVED:
                return mResult;
        }
    }

    private void blockWhilePending() throws InterruptedException {
        synchronized (mBlockingObject) {
            while (isPending()) {
                try {
                    mBlockingObject.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    @Override
    public Promise<Result, Progress> done(final DoneCallback<Result> callback) {
        int state;
        synchronized (this) {
            state = mState;
            if (state == STATE_PENDING) {
                mCallbacks.mDoneCallbacks.add(callback);
            }
        }

        if (state == STATE_RESOLVED) {
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
        }
        return this;
    }

    private boolean shouldExecuteInBackground(DoneCallback<Result> callback) {
        return mThreadHelper.isCurrentThread() && !(callback instanceof UIDoneCallback);
    }

    @Override
    public Promise<Result, Progress> fail(final FailCallback callback) {
        int state;
        synchronized (this) {
            state = mState;
            if (state == STATE_PENDING) {
                mCallbacks.mFailCallbacks.add(callback);
            }
        }

        if (state == STATE_REJECTED) {
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
        }
        return this;
    }

    private boolean shouldExecuteInBackground(FailCallback callback) {
        return mThreadHelper.isCurrentThread() && !(callback instanceof UIFailCallback);
    }

    @Override
    public Promise<Result, Progress> progress(final ProgressCallback<Progress> callback) {
        synchronized (this) {
            switch (mState) {
                case STATE_PENDING:
                    mCallbacks.mProgressCallbacks.add(callback);
                    break;
            }
        }
        return this;
    }

    @Override
    public Promise<Result, Progress> cancel(final CancelCallback callback) {
        int state;
        synchronized (this) {
            state = mState;
            if (mState == STATE_PENDING) {
                mCallbacks.mCancelCallbacks.add(callback);
            }
        }
        if (state == STATE_CANCELLED) {
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
        }
        return this;
    }

    private boolean shouldExecuteInBackground(CancelCallback callback) {
        return mThreadHelper.isCurrentThread() && !(callback instanceof UICancelCallback);
    }

    @Override
    public Promise<Result, Progress> always(final AlwaysCallback callback) {
        int state;
        synchronized (this) {
            state = mState;
            if (state == STATE_PENDING) {
                mCallbacks.mAlwaysCallbacks.add(callback);
            }
        }
        switch (state) {
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
        return this;
    }

    @Override
    public <R, P> Promise<R, P> then(final ThenCallback<Result, R, P> callback) {
        final ResolvablePromise<R, P> promise = mAwex.newAwexPromise();

        fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                reject(exception);
            }
        }).done(new DoneCallback<Result>() {
            @Override
            public void onDone(Result result) {
                callback.then(result).pipe(promise);
            }
        });

        return promise;
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

    @Override
    public Promise<Result, Progress> pipe(final Promise<Result, Progress> promise) {
        if (!(promise instanceof AwexPromise)) {
            throw new IllegalArgumentException("Trying to do a pipe with a non Awex promise.");
        }

        final AwexPromise<Result, Progress> awexPromise = (AwexPromise<Result, Progress>) promise;

        done(new DoneCallback<Result>() {
            @Override
            public void onDone(Result result) {
                synchronized (awexPromise) {
                    if (awexPromise.isPending()) {
                        awexPromise.resolve(result);
                    }
                }
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                synchronized (awexPromise) {
                    if (awexPromise.isPending()) {
                        awexPromise.reject(exception);
                    }
                }
            }
        }).progress(new ProgressCallback<Progress>() {
            @Override
            public void onProgress(Progress progress) {
                synchronized (awexPromise) {
                    if (awexPromise.isPending()) {
                        awexPromise.notifyProgress(progress);
                    }
                }
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                awexPromise.cancelTask();
            }
        });
        return promise;
    }

    private boolean shouldExecuteInBackground(AlwaysCallback callback) {
        return mThreadHelper.isCurrentThread() && !(callback instanceof UIAlwaysCallback);
    }

    private void printStateChanged(String newState) {
        if (mLogger.isEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            mLogger.v(stringBuilder.append("Promise of task ")
                    .append(mId)
                    .append(" changed to state ")
                    .append(newState)
                    .toString());
        }
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
