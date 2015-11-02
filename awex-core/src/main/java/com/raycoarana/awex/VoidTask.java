package com.raycoarana.awex;

/**
 * Base class for tasks that doesn't returns any result.
 *
 * @see Task
 */
public abstract class VoidTask extends Task<Void> {

    public VoidTask() {
        super();
    }

    public VoidTask(int priority) {
        super(priority);
    }

    @Override
    protected Void run() throws InterruptedException {
        runWithoutResult();
        return null;
    }

    protected abstract void runWithoutResult() throws InterruptedException;

}