package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.AlwaysCallback;
import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.transform.Filter;
import com.raycoarana.awex.transform.Mapper;

import java.util.Collection;

public interface Promise<T> {

    /**
     * State: Task associated with this promise is executing and did not finish already
     */
    int STATE_PENDING = 0;

    /**
     * State: Task associated with this promise has finish executing and have a result
     */
    int STATE_RESOLVED = 1;

    /**
     * State: Task associated with this promise has an error while executing
     */
    int STATE_REJECTED = 2;

    /**
     * State: Task associated with this promise is cancelled
     */
    int STATE_CANCELLED = 3;

    /**
     * Cancels the task associated with the promise, no callbacks will be executed after the execution of this method
     * and even dispatched callbacks to UI thread will be mark to be ignored. You could expect no side effects of any
     * callback after this call.
     */
    void cancelTask();

    void cancelTask(boolean mayInterrupt);

    /**
     * Gets the current state of the promise
     *
     * @return an integer value from: STATE_PENDING, STATE_RESOLVED, STATE_REJECTED, STATE_CANCELLED
     */
    int getState();

    boolean isPending();

    boolean isResolved();

    boolean isRejected();

    boolean isCancelled();

    /**
     * This promise is completed in either state: resolved, rejected or cancelled
     *
     * @return true if the state of the promise is resolved, rejected or cancelled or false otherwise
     */
    boolean isCompleted();

    /**
     * Will block the current thread until the promise if resolved, rejected or cancelled. It will return
     * the value of the promise in case of resolved or throw an exception in any other case.
     *
     * @return the result of the task if any
     * @throws IllegalStateException if the state of the promise is STATE_CANCELLED
     * @throws Exception             an exception if the task fails to execute
     */
    T getResult() throws Exception;

    /**
     * Will block the current thread until the promise if resolved, rejected or cancelled. It will return
     * the value of the promise in case of resolved or return the defaultValue in any other case.
     *
     * @param defaultValue default value to return in case that the promise was rejected or cancelled
     * @return the result of the task if any
     */
    T getResultOrDefault(T defaultValue) throws InterruptedException;

    Promise<T> done(DoneCallback<T> callback);

    Promise<T> fail(FailCallback callback);

    Promise<T> progress(ProgressCallback callback);

    Promise<T> cancel(CancelCallback callback);

    Promise<T> always(AlwaysCallback callback);

    /**
     * Returns a promise that will be resolved with the value of the first resolved promise or
     * fail if both promises (current and provided as parameter) fails
     *
     * @param promise
     * @return
     */
    Promise<T> or(Promise<T> promise);

    /**
     * Returns a promise that will be resolved if both promises have values or fail if the current
     * promise or the promise of the parameter fails.
     *
     * @param promise
     * @return
     */
    Promise<Collection<T>> and(Promise<T> promise);

    <U> Promise<U> mapSingle(Mapper<T, U> mapper);

    /**
     * Filters the item, if the result of the promise does not match the filter, the promise is rejected
     *
     * @param filter
     * @return
     */
    Promise<T> filterSingle(Filter<T> filter);

    /**
     * Creates a collection promise from this promise. If this promise result is a collection,
     * it will be converted, otherwise, the result will be inserted in a collection.
     *
     * @return
     */
    <U> CollectionPromise<U> stream();

}