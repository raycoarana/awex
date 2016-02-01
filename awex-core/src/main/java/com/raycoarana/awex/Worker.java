package com.raycoarana.awex;

import com.raycoarana.awex.state.WorkerState;
import com.raycoarana.awex.state.WorkerState.State;

class Worker implements Runnable {

    private final long mId;
    private final Thread mThread;
    private final AwexTaskQueue mWorkQueue;
    private final Logger mLogger;
    private final WorkerListener mListener;

    private boolean mExecutingTask;
    private boolean mDie = false;
    private Task mCurrentTask;
    private long mLastTimeActive;

    public Worker(long id, AwexTaskQueue workQueue, Logger logger, WorkerListener listener) {
        mId = id;
        mThread = new Thread(this, "Awex worker " + id);
        mWorkQueue = workQueue;
        mLogger = logger;
        mListener = listener;

        mThread.start();
    }

    public long getId() {
        return mId;
    }

    @Override
    public void run() {
        mLogger.v("Worker " + mId + " starting...");
        try {
            while (!mDie) {
                try {
                    Task newTask = mWorkQueue.take(this);
                    synchronized (this) {
                        mCurrentTask = newTask;
                        mExecutingTask = true;
                        mLastTimeActive = mCurrentTask != null ? System.nanoTime() / 10000000 : mLastTimeActive;
                    }
                    if (mCurrentTask != null) {
                        long taskId = mCurrentTask.getId();
                        mLogger.v("Worker " + mId + " start executing task " + taskId);
                        mCurrentTask.execute();
                        mLogger.v("Worker " + mId + " ends executing task " + taskId);
                    }
                } catch (InterruptedException ex) {
                    return;
                } finally {
                    Task executedTask = mCurrentTask;
                    synchronized (this) {
                        mCurrentTask = null;
                        mExecutingTask = false;
                    }
                    if (executedTask != null) {
                        mListener.onTaskFinished(executedTask);
                    }
                }
            }
        } finally {
            mLogger.v("Worker " + mId + " dies");
        }
    }

    public synchronized WorkerState takeState() {
        return new WorkerState(getState(), mCurrentTask, mLastTimeActive);
    }

    private State getState() {
        if (!mExecutingTask) {
            return State.WAITING_FOR_NEXT_TASK;
        }

        Thread.State state = mThread.getState();
        switch (state) {
            case NEW:
                return State.NEW;
            case RUNNABLE:
                return State.RUNNABLE;
            case BLOCKED:
                return State.BLOCKED;
            case WAITING:
                return State.WAITING;
            case TERMINATED:
                return State.TERMINATED;
            case TIMED_WAITING:
                return State.TIMED_WAITING;
        }
        throw new IllegalStateException("Worker in an illegal state");
    }

    public void interrupt() {
        die();
        mThread.interrupt();
    }

    public void die() {
        mDie = true;
    }

}