package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class AbstractMultiThreadPromise<T, U, Progress> extends AbstractSingleThreadPromise<T, U, Progress> {

    public AbstractMultiThreadPromise(Awex awex, CollectionPromise<T, Progress> promise, Apply<T, U> filter) {
        super(awex, promise, filter);
    }

    @Override
    protected void apply(Collection items) {
        int numberOfThreads = mAwex.getNumberOfThreads();
        Collection<List> itemsGroupedByThread = split(items, numberOfThreads);
        Collection<Promise<Collection<U>, Progress>> promises = launchAll(itemsGroupedByThread);
        AfterAllPromise<Collection<U>, Progress> afterAll = new AfterAllPromise<>(mAwex, promises);
        afterAll.done(new DoneCallback<MultipleResult<Collection<U>, Progress>>() {
            @Override
            public void onDone(MultipleResult<Collection<U>, Progress> result) {
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

    @SuppressWarnings("unchecked")
    private Collection<List> split(Iterable items, int groups) {
        List<List> listOfGroups = new ArrayList<>();
        for (int i = 0; i < groups; i++) {
            listOfGroups.add(new ArrayList());
        }
        int itemIndex = 0;
        for (Object item : items) {
            listOfGroups.get(itemIndex % groups).add(item);
            itemIndex++;
        }

        return listOfGroups;
    }

    private Collection<Promise<Collection<U>, Progress>> launchAll(Collection<List> itemsGroupedByThread) {
        List<Promise<Collection<U>, Progress>> allPromises = new ArrayList<>();
        for (final Collection items : itemsGroupedByThread) {
            Promise<Collection<U>, Progress> promise = mAwex.submit(new Task<Collection<U>, Progress>() {
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
