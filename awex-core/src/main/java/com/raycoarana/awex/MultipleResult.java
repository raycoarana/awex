package com.raycoarana.awex;

public class MultipleResult<T> {

    private final Promise<T>[] mPromises;
    private final T[] mResults;
    private final Exception[] mErrors;

    public MultipleResult(Promise<T>[] promises, T[] results, Exception[] errors) {
        mPromises = promises;
        mResults = results;
        mErrors = errors;
    }

    public T getResult(int index) throws Exception {
        switch (getState(index)) {
            case Promise.STATE_PENDING:
            case Promise.STATE_CANCELLED:
                throw new IllegalStateException("Invalid state of promise");
            case Promise.STATE_REJECTED:
                throw mErrors[index];
            default:
                return mResults[index];
        }
    }

    public T getResultOrDefault(int index, T defaultValue) {
        switch (getState(index)) {
            case Promise.STATE_PENDING:
            case Promise.STATE_CANCELLED:
                throw new IllegalStateException("Invalid state of promise");
            case Promise.STATE_REJECTED:
                return defaultValue;
            default:
                return mResults[index];
        }
    }

    public int getState(int index) {
        return mPromises[index].getState();
    }

    public int getCount() {
        return mResults.length;
    }

}