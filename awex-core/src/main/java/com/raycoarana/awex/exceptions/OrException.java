package com.raycoarana.awex.exceptions;

public class OrException extends Exception {

    private final Exception mMainException;
    private final Exception mSecondChoiseException;

    public OrException(Exception mainException, Exception secondChoiseException) {
        super("Both promises of OR operation rejected");
        mMainException = mainException;
        mSecondChoiseException = secondChoiseException;
    }

    public Exception getMainPromiseException() {
        return mMainException;
    }

    public Exception getSecondChoisePromiseException() {
        return mSecondChoiseException;
    }

}