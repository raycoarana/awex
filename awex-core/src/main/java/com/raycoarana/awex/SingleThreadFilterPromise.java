package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.transform.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SingleThreadFilterPromise<T> extends AwexPromise<Collection<T>> {

    protected final Filter<T> mFilter;

    @SuppressWarnings("unchecked")
    public SingleThreadFilterPromise(Awex awex, Promise promise, Filter<T> filter) {
        super(awex);

        mFilter = filter;
        promise.done(new DoneCallback() {
            @Override
            public void onDone(Object result) {
                SingleThreadFilterPromise.this.doFilter(result);
            }
        }).fail(new FailCallback() {
            @Override
            public void onFail(Exception exception) {
                SingleThreadFilterPromise.this.reject(exception);
            }
        }).cancel(new CancelCallback() {
            @Override
            public void onCancel() {
                SingleThreadFilterPromise.this.cancelWork();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void doFilter(Object result) {
        if (result instanceof Iterable) {
            filterCollection((Iterable<T>) result);
        } else {
            filterSingle((T) result);
        }
    }

    protected void filterCollection(Iterable<T> items) {
        Collection<T> filteredItems = new ArrayList<>();
        for (T item : items) {
            if (mFilter.filter(item)) {
                filteredItems.add(item);
            }
        }
        resolve(filteredItems);
    }

    private void filterSingle(T result) {
        if (mFilter.filter(result)) {
            resolve(Collections.singletonList(result));
        } else {
            resolve(Collections.<T>emptyList());
        }
    }

}
