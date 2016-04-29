package com.raycoarana.awex;

public interface ResolvablePromise<Result, Progress> extends Promise<Result, Progress>{

    /**
     * Resolves the promise, triggers any done/always callbacks
     *
     * @param result value used to resolve the promise
     * @throws IllegalStateException if the promise is not in pending state
     * @return this promise
     */
    Promise<Result, Progress> resolve(Result result);

    /**
     * Rejects the promise, triggers any fail/always callbacks
     *
     * @param ex exception that represents the rejection of the promise
     * @return this promise
     */
    Promise<Result, Progress> reject(Exception ex);

    /**
     * Notify progress to all callbacks
     *
     * @param progress amount of progress
     */
    void notifyProgress(Progress progress);

}

