package com.raycoarana.awex;

public class MultipleResult<Result, Progress> {

    private final Promise<Result, Progress>[] mPromises;
    private final Result[] mResults;
    private final Exception[] mErrors;

    public MultipleResult(Promise<Result, Progress>[] promises, Result[] results, Exception[] errors) {
        mPromises = promises;
        mResults = results;
        mErrors = errors;
    }

    public Result getResult(int index) throws Exception {
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

    public Result getResultOrDefault(int index, Result defaultValue) {
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