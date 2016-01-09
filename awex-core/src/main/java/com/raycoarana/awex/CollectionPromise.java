package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Func;
import com.raycoarana.awex.transform.Mapper;

import java.util.Collection;

public interface CollectionPromise<T> extends Promise<Collection<T>> {

    CollectionPromise<T> filter(Filter<T> filter);

    CollectionPromise<T> filterParallel(Filter<T> filter);

    <U> CollectionPromise<U> map(Mapper<T, U> mapper);

    <U> CollectionPromise<U> mapParallel(Mapper<T, U> mapper);

    CollectionPromise<T> forEach(Func<T> func);

    CollectionPromise<T> forEachParallel(Func<T> func);

}
