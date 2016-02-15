package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Func;
import com.raycoarana.awex.transform.Mapper;

import java.util.Collection;

public interface CollectionPromise<T, P> extends Promise<Collection<T>, P> {

    CollectionPromise<T, P> filter(Filter<T> filter);

    CollectionPromise<T, P> filterParallel(Filter<T> filter);

    <U> CollectionPromise<U, P> map(Mapper<T, U> mapper);

    <U> CollectionPromise<U, P> mapParallel(Mapper<T, U> mapper);

    CollectionPromise<T, P> forEach(Func<T> func);

    CollectionPromise<T, P> forEachParallel(Func<T> func);

    Promise<T, P> singleOrFirst();

    CollectionPromise<T, P> applyNow();
}
