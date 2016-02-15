package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

abstract class AbstractSingleThreadPromise<T, U, P> extends AwexCollectionPromise<U, P> {

    protected final Apply[] mApplyChain;
    protected final Promise mChainStarterPromise;
    private boolean mIsAttach;

    @SuppressWarnings("unchecked")
    public AbstractSingleThreadPromise(Awex awex, CollectionPromise<T, P> promise, Apply<T, U> apply) {
        super(awex);

        if (promise instanceof AbstractSingleThreadPromise && !((AbstractSingleThreadPromise) promise).mIsAttach) {
            AbstractSingleThreadPromise abstractSingleThreadPromise = (AbstractSingleThreadPromise) promise;
            Apply[] applyChain = abstractSingleThreadPromise.mApplyChain;
            mApplyChain = Arrays.copyOf(applyChain, applyChain.length + 1);
            mApplyChain[applyChain.length] = apply;
            mChainStarterPromise = abstractSingleThreadPromise.mChainStarterPromise;
        } else {
            mApplyChain = new Apply[]{apply};
            mChainStarterPromise = promise;
        }

        mChainStarterPromise.fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                AbstractSingleThreadPromise.this.reject(exception);
            }
        });
        mChainStarterPromise.cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                AbstractSingleThreadPromise.this.cancelTask();
            }
        });
    }

    @Override
    public Promise<Collection<U>, P> done(DoneCallback<Collection<U>> callback) {
        attachIfNecessary();
        return super.done(callback);
    }

    @Override
    public Collection<U> getResult() throws Exception {
        attachIfNecessary();
        return super.getResult();
    }

    @Override
    public Collection<U> getResultOrDefault(Collection<U> defaultValue) throws InterruptedException {
        attachIfNecessary();
        return super.getResultOrDefault(defaultValue);
    }

    @Override
    public Promise<U, P> singleOrFirst() {
        return super.singleOrFirst();
    }

    @Override
    public CollectionPromise<U, P> applyNow() {
        attachIfNecessary();
        return this;
    }

    @SuppressWarnings("unchecked")
    private void attachIfNecessary() {
        if (mIsAttach) {
            return; //fast return without monitoring
        }
        synchronized (this) {
            if (mIsAttach) {
                return;
            }
            mIsAttach = true;
            mChainStarterPromise.done(new DoneCallback<Collection>() {
                @Override
                public void onDone(Collection result) {
                    AbstractSingleThreadPromise.this.apply(result);
                }
            });
        }
    }

    protected void apply(Collection items) {
        Collection<U> results = applyToCollection(items);
        resolve(results);
    }

    @SuppressWarnings("unchecked")
    protected Collection<U> applyToCollection(Iterable items) {
        Collection<U> results = new ArrayList<>();
        for (Object item : items) {
            boolean shouldBeAdded = true;
            for (Apply apply : mApplyChain) {
                if (apply.shouldApply(item)) {
                    item = apply.apply(item);
                } else {
                    shouldBeAdded = false;
                    break;
                }
            }

            if (shouldBeAdded) {
                results.add((U) item);
            }
        }
        return results;
    }

}
