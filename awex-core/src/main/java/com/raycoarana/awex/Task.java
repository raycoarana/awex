package com.raycoarana.awex;

import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

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

    private Awex mAwex;
    private long mId;
    private Logger mLogger;
    private AwexPromise<T> mPromise;
    private int mCurrentState = STATE_NOT_INITIALIZED;
    private Worker mWorker;
    private AwexTaskQueue mTaskQueue;
    private final int mQueueTimeout;
    private TimerTask mQueueTimeoutTimerTask;
    private final int mExecutionTimeout;
    private TimerTask mExecutionTimeoutTimerTask;

    public Task() {
        this(PRIORITY_NORMAL, -1, -1);
    }

    public Task(int priority) {
        this(priority, -1, -1);
    }

    public Task(int priority, int queueTimeout, int executionTimeout) {
        mPriority = priority;
        mQueueTimeout = queueTimeout;
        mExecutionTimeout = executionTimeout;
    }

    void initialize(Awex awex) {
        if (mCurrentState != STATE_NOT_INITIALIZED) {
            throw new IllegalStateException("Trying to reuse an already submitted task");
        }

        mAwex = awex;
        mId = awex.provideWorkId();
        mLogger = awex.provideLogger();

        mCurrentState = STATE_NOT_QUEUE;
        printStateChanged("NOT_QUEUE");

        mPromise = new AwexPromise<>(awex, this);

        mQueueTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (mTaskQueue != null && mTaskQueue.remove(Task.this)) {
                    mAwex.onTaskQueueTimeout(Task.this);
                }
            }
        };
        mExecutionTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                mAwex.onTaskExecutionTimeout(Task.this);
            }
        };
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
        return mCurrentState == STATE_CANCELLING || mCurrentState == STATE_CANCELLED;
    }

    public void reset() {
        try {
            lock.lock();

            if (mCurrentState != STATE_FINISHED) {
                throw new IllegalStateException("Trying to reuse an already submitted task");
            }
            mCurrentState = STATE_NOT_INITIALIZED;
            onReset();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Override this method to reset any state of the task prior to reuse it
     */
    protected void onReset() {
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

    final ReentrantLock lock = new ReentrantLock();

    final void execute() throws InterruptedException {
        checkInitialized();

        mQueueTimeoutTimerTask.cancel();
        mCurrentState = STATE_RUNNING;
        printStateChanged("RUNNING");
        mAwex.schedule(mExecutionTimeoutTimerTask, mExecutionTimeout);

        T result = null;
        try {
            result = run();
        } catch (InterruptedException ex) {
            if (mPromise.isCancelled()) {
                mCurrentState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
            }
            Thread.currentThread().interrupt();
            throw ex;
        } catch (Exception ex) {
            mPromise.reject(ex);
        } finally {
            mExecutionTimeoutTimerTask.cancel();
        }

        resolveWithResult(result);
    }

    private void resolveWithResult(T result) {
        try {
            lock.lock();

            if (mPromise.isPending()) {
                mPromise.resolve(result);
            }

            if (mCurrentState == STATE_CANCELLING) {
                mCurrentState = STATE_CANCELLED;
                printStateChanged("CANCELLED");
                return;
            }
            mCurrentState = STATE_FINISHED;
            printStateChanged("FINISHED");
        } finally {
            mWorker = null;
            lock.unlock();
        }
    }

    final void softCancel() {
        try {
            lock.lock();
            checkInitialized();

            mCurrentState = STATE_CANCELLING;
            printStateChanged("CANCELLING");
        } finally {
            lock.unlock();
        }
    }

    final void markQueue(AwexTaskQueue taskQueue) {
        checkInitialized();

        mTaskQueue = taskQueue;
        mCurrentState = STATE_QUEUE;
        printStateChanged("QUEUE");
        mAwex.schedule(mQueueTimeoutTimerTask, mQueueTimeout);
    }

    private void printStateChanged(String newState) {
        mLogger.v("Task " + mId + " state changed to " + newState);
    }

    final Worker getWorker() {
        return mWorker;
    }

    final void setWorker(Worker worker) {
        mWorker = worker;
    }

    final AwexTaskQueue getQueue() {
        return mTaskQueue;
    }


    public void toString(StringBuilder stringBuilder) {
        String taskName = getClass().getSimpleName();
        stringBuilder.append("{/*")
                .append(taskName)
                .append("*/ id: ")
                .append(mId)
                .append(", state: ")
                .append(mCurrentState)
                .append(", priority: ")
                .append(mPriority)
                .append(" }");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        toString(stringBuilder);
        return stringBuilder.toString();
    }
}