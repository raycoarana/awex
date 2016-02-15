package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.AbsentValueException;

import java.util.Arrays;

class AbstractTransformerPromise<T, U, P> extends AwexPromise<U, P> {

    protected final Apply[] mApplyChain;
    protected final Promise mChainStarterPromise;

    public AbstractTransformerPromise(Awex awex, Promise<T, P> promise, Apply<T, U> apply) {
        super(awex);

        if (promise instanceof AbstractTransformerPromise) {
            AbstractTransformerPromise abstractTransformerPromise = (AbstractTransformerPromise) promise;
            Apply[] applyChain = abstractTransformerPromise.mApplyChain;
            mApplyChain = Arrays.copyOf(applyChain, applyChain.length + 1);
            mApplyChain[applyChain.length] = apply;
            mChainStarterPromise = abstractTransformerPromise.mChainStarterPromise;
        } else {
            mApplyChain = new Apply[]{apply};
            mChainStarterPromise = promise;
        }

        mChainStarterPromise.done(new DoneCallback() {
            @Override
            public void onDone(Object result) {
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

    @SuppressWarnings("unchecked")
    private void apply(Object item) {
        for (Apply apply : mApplyChain) {
            if (apply.shouldApply(item)) {
                item = apply.apply(item);
            } else {
                reject(new AbsentValueException());
                return;
            }
        }

        resolve((U) item);
    }

    @Override
    public Promise<U, P> done(DoneCallback<U> callback) {
        return super.done(callback);
    }

}
