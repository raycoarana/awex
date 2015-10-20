package com.raycoarana.awex;

/**
 * Base class for works that doesn't returns any result.
 *
 * @see Work
 */
public abstract class VoidWork extends Work<Void> {

    public VoidWork(Awex awex) {
        super(awex);
    }

    @Override
    protected Void run() throws InterruptedException {
        runWithoutResult();
        return null;
    }

    protected abstract void runWithoutResult() throws InterruptedException;

}