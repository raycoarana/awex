package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.OrException;

class OrPromise<T> extends AwexPromise<T> {

    private final Promise<T> mMainPromise;
    private final Promise<T> mSecondChoicePromise;

    public OrPromise(Awex awex, Promise<T> mainPromise, Promise<T> secondChoicePromise) {
        super(awex);

        mMainPromise = mainPromise;
        mSecondChoicePromise = secondChoicePromise;

        final CancelCallback cancelCallback = buildCancelCallback();

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
                                .cancel(cancelCallback);
                    }
                })
                .cancel(cancelCallback);
    }

    private CancelCallback buildCancelCallback() {
        return new CancelCallback() {
            @Override
            public void onCancel() {
                synchronized (OrPromise.this) {
                    if (getState() == STATE_PENDING) {
                        cancel();
                    }
                }
            }
        };
    }

    @Override
    public void cancel() {
        synchronized (this) {
            super.cancel();
            mMainPromise.cancel();
            mSecondChoicePromise.cancel();
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
}