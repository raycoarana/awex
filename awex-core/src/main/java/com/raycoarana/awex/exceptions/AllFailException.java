package com.raycoarana.awex.exceptions;

public class AllFailException extends Exception {

    private Exception[] mExceptions;

    public AllFailException(Exception... exceptions) {
        mExceptions = exceptions;
    }

    public Exception getException(int index) {
        return mExceptions[index];
    }

    public int getCount() {
        return mExceptions.length;
    }

}
