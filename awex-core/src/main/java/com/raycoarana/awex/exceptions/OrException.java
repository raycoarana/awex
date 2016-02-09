package com.raycoarana.awex.exceptions;

public class OrException extends Exception {

    private final Exception mMainException;
    private final Exception mSecondChoiceException;

    public OrException(Exception mainException, Exception secondChoiceException) {
        super("Both promises of OR operation rejected");
        mMainException = mainException;
        mSecondChoiceException = secondChoiceException;
    }

    public Exception getMainPromiseException() {
        return mMainException;
    }

    public Exception getSecondChoicePromiseException() {
        return mSecondChoiceException;
    }

}