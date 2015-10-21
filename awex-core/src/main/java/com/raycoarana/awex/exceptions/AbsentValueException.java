package com.raycoarana.awex.exceptions;

public class AbsentValueException extends Exception {

    public AbsentValueException() {
        super("Promise rejected without any value");
    }

}