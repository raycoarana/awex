package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.transform.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MultiThreadFilterPromise<T> extends SingleThreadFilterPromise<T> {

    @SuppressWarnings("unchecked")
    public MultiThreadFilterPromise(Awex awex, Promise promise, Filter<T> filter) {
        super(awex, promise, filter);
    }

    @Override
    protected void filterCollection(Iterable<T> items) {
        int numberOfThreads = mAwex.getNumberOfThreads();
        Collection<List<T>> itemsGroupedByThread = split(items, numberOfThreads);
        Collection<Promise<Collection<T>>> promises = launchAll(itemsGroupedByThread);
        AfterAllPromise<Collection<T>> afterAll = new AfterAllPromise<>(mAwex, promises);
        afterAll.done(new DoneCallback<MultipleResult<Collection<T>>>() {
            @Override
            public void onDone(MultipleResult<Collection<T>> result) {
                Collection<T> filteredItems = new ArrayList<>();
                for (int i = 0; i < result.getCount(); i++) {
                    Collection<T> items = result.getResultOrDefault(i, Collections.<T>emptyList());
                    filteredItems.addAll(items);
                }
                resolve(filteredItems);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                MultiThreadFilterPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                MultiThreadFilterPromise.this.cancelWork();
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

    private Collection<Promise<Collection<T>>> launchAll(Collection<List<T>> itemsGroupedByThread) {
        List<Promise<Collection<T>>> allPromises = new ArrayList<>();
        for (final Collection<T> items : itemsGroupedByThread) {
            Promise<Collection<T>> promise = mAwex.submit(new Work<Collection<T>>() {
                @Override
                protected Collection<T> run() throws InterruptedException {
                    return processItems(items);
                }
            });
            allPromises.add(promise);
        }
        return allPromises;
    }

    private Collection<T> processItems(Collection<T> items) {
        Collection<T> filteredItems = new ArrayList<>();
        for (T item : items) {
            if (mFilter.filter(item)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

}
