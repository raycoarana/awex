package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.OrException;

class OrPromise<T, P> extends AwexPromise<T, P> {

    private final Promise<T, P> mMainPromise;
    private final Promise<T, P> mSecondChoicePromise;

    public OrPromise(Awex awex, Promise<T, P> mainPromise, Promise<T, P> secondChoicePromise) {
        super(awex);

        mMainPromise = mainPromise;
        mSecondChoicePromise = secondChoicePromise;

        mainPromise.done(mDoneCallback)
                .fail(new FailCallback() {
                    @Override
                    public void onFail(final Exception mainException) {
                        mSecondChoicePromise.done(mDoneCallback)
                                .fail(new FailCallback() {
                                    @Override
                                    public void onFail(Exception secondChoiseException) {
                                        synchronized (OrPromise.this) {
                                            if (getState() == STATE_PENDING) {
                                                reject(new OrException(mainException, secondChoiseException));
                                            }
                                        }
                                    }
                                })
                                .cancel(mCancellCallback);
                    }
                })
                .cancel(mCancellCallback);
    }

    @Override
    public void cancelTask(boolean mayInterrupt) {
        synchronized (this) {
            super.cancelTask(mayInterrupt);
            mMainPromise.cancelTask(mayInterrupt);
            mSecondChoicePromise.cancelTask(mayInterrupt);
        }
    }

    private final DoneCallback<T> mDoneCallback = new DoneCallback<T>() {
        @Override
        public void onDone(T result) {
            synchronized (OrPromise.this) {
                if (getState() == STATE_PENDING) {
                    resolve(result);
                }
            }
        }
    };

    private final CancelCallback mCancellCallback = new CancelCallback() {
        @Override
        public void onCancel() {
            synchronized (OrPromise.this) {
                if (getState() == STATE_PENDING) {
                    cancelTask(false);
                }
            }
        }
    };
}