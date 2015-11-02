package com.raycoarana.awex;

/**
 * Base class for any task that will create some object as a result. Make sure you implement a safe way to cancel this
 * task when the state is changed to STATE_CANCELLING. Tasks will have some minor time to successfully exit before
 * a more aggressive cancel will be tried (interrupting the thread). You should not catch any InterruptedException
 * and let it abort the task, just do whatever you need to safely abort the task.
 *
 * @see VoidTask for tasks that doesn't returns any object
 */
public abstract class Task<T> {

    public static final int STATE_NOT_INITIALIZED = -1;
    public static final int STATE_NOT_QUEUE = 0;
    public static final int STATE_QUEUE = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_FINISHED = 3;
    public static final int STATE_CANCELLING = 4;
    public static final int STATE_CANCELLED = 5;

    public static final int PRIORITY_LOWEST = 1;
    public static final int PRIORITY_LOW = 2;
    public static final int PRIORITY_NORMAL = 3;
    public static final int PRIORITY_HIGH = 4;
    public static final int PRIORITY_REAL_TIME = Integer.MAX_VALUE;

    private final int mPriority;

    private Logger mLogger;
    private long mId;
    private AwexPromise<T> mPromise;
    private int mCurrentState = STATE_NOT_INITIALIZED;

    public Task() {
        this(PRIORITY_NORMAL);
    }

    public Task(int priority) {
        mPriority = priority;
    }

    void initialize(Awex awex) {
        if (mCurrentState != STATE_NOT_INITIALIZED) {
            throw new IllegalStateException("Trying to reuse an already submitted task");
        }

        mId = awex.provideWorkId();
        mLogger = awex.provideLogger();

        mCurrentState = STATE_NOT_QUEUE;
        printStateChanged("NOT_QUEUE");

        mPromise = new AwexPromise<>(awex, this);
    }

    public long getId() {
        checkInitialized();
        return mId;
    }

    public int getPriority() {
        return mPriority;
    }

    public int getState() {
        return mCurrentState;
    }

    public boolean isCancelled() {
        return mCurrentState == STATE_CANCELLED;
    }

    public void reset() {
        if (mCurrentState != STATE_FINISHED) {
            throw new IllegalStateException("Trying to reuse an already submitted task");
        }
    }

    public Promise<T> getPromise() {
        checkInitialized();
        return mPromise;
    }

    private void checkInitialized() {
        if (mCurrentState == STATE_NOT_INITIALIZED) {
            throw new IllegalStateException("Task not already initialized, before calling this method ensure this task is submitted");
        }
    }

    protected abstract T run() throws InterruptedException;

    final void execute() throws InterruptedException {
        checkInitialized();

        mCurrentState = STATE_RUNNING;
        printStateChanged("RUNNING");
        try {
            T result = run();
            if (mCurrentState == STATE_CANCELLING) {
                mCurrentState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
                return;
            }
            mPromise.resolve(result);
        } catch (InterruptedException ex) {
            if (mPromise.isCancelled()) {
                mCurrentState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
            }
            Thread.currentThread().interrupt();
            throw ex;
        } catch (Exception ex) {
            mPromise.reject(ex);
        }
        mCurrentState = STATE_FINISHED;
        printStateChanged("FINISHED");
    }

    final void softCancel() {
        checkInitialized();

        mCurrentState = STATE_CANCELLING;
        printStateChanged("CANCELLING");
    }

    final void markQueue() {
        checkInitialized();

        mCurrentState = STATE_QUEUE;
        printStateChanged("QUEUE");
    }

    private void printStateChanged(String newState) {
        mLogger.v("Task " + mId + " state changed to " + newState);
    }
}