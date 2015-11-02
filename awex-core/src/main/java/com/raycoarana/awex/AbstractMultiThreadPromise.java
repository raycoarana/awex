package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AbstractMultiThreadPromise<T, U> extends AbstractSingleThreadPromise<T, U> {

    @SuppressWarnings("unchecked")
    public AbstractMultiThreadPromise(Awex awex, Promise promise, Apply<T, U> filter) {
        super(awex, promise, filter);
    }

    @Override
    protected void applyToCollectionAndResolve(Iterable<T> items) {
        int numberOfThreads = mAwex.getNumberOfThreads();
        Collection<List<T>> itemsGroupedByThread = split(items, numberOfThreads);
        Collection<Promise<Collection<U>>> promises = launchAll(itemsGroupedByThread);
        AfterAllPromise<Collection<U>> afterAll = new AfterAllPromise<>(mAwex, promises);
        afterAll.done(new DoneCallback<MultipleResult<Collection<U>>>() {
            @Override
            public void onDone(MultipleResult<Collection<U>> result) {
                Collection<U> resultItems = new ArrayList<>();
                for (int i = 0; i < result.getCount(); i++) {
                    Collection<U> items = result.getResultOrDefault(i, Collections.<U>emptyList());
                    resultItems.addAll(items);
                }
                resolve(resultItems);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                AbstractMultiThreadPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                AbstractMultiThreadPromise.this.cancelTask();
            }
        });
    }

    private Collection<List<T>> split(Iterable<T> items, int groups) {
        List<List<T>> listOfGroups = new ArrayList<>();
        for (int i = 0; i < groups; i++) {
            listOfGroups.add(new ArrayList<T>());
        }
        int itemIndex = 0;
        for (T item : items) {
            listOfGroups.get(itemIndex % groups).add(item);
            itemIndex++;
        }

        return listOfGroups;
    }

    private Collection<Promise<Collection<U>>> launchAll(Collection<List<T>> itemsGroupedByThread) {
        List<Promise<Collection<U>>> allPromises = new ArrayList<>();
        for (final Collection<T> items : itemsGroupedByThread) {
            Promise<Collection<U>> promise = mAwex.submit(new Task<Collection<U>>() {
                @Override
                protected Collection<U> run() throws InterruptedException {
                    return AbstractMultiThreadPromise.super.applyToCollection(items);
                }
            });
            allPromises.add(promise);
        }
        return allPromises;
    }

}
