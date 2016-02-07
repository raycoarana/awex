package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;

import java.util.LinkedList;
import java.util.Queue;

public class WorkerStateImpl implements WorkerState {

    private int mId;
    private State mState;
    private Task mCurrentTask;
    private long mLastTimeActive;

    private final static Queue<WorkerStateImpl> sObjectPool = new LinkedList<>();

    public static WorkerStateImpl get(int id, State state, Task currentTask, long lastTimeActive) {
        WorkerStateImpl workerState;
        synchronized (sObjectPool) {
            if (sObjectPool.size() > 0) {
                workerState = sObjectPool.poll();
            } else {
                workerState = new WorkerStateImpl();
            }
            workerState.mId = id;
            workerState.mState = state;
            workerState.mCurrentTask = currentTask;
            workerState.mLastTimeActive = lastTimeActive;
        }
        return workerState;
    }

    private WorkerStateImpl() {
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public Task getCurrentTask() {
        return mCurrentTask;
    }

    @Override
    public long getLastTimeActive() {
        return mLastTimeActive;
    }

    public void recycle() {
        mCurrentTask = null;
        synchronized (sObjectPool) {
            sObjectPool.add(this);
        }
    }

    public void toString(StringBuilder stringBuilder) {
        stringBuilder.append("{/*Worker*/ id: ")
                .append(mId)
                .append(", state: ")
                .append(mState)
                .append(", lastTimeActive: ")
                .append(mLastTimeActive)
                .append(", currentTask: ");
        if (mCurrentTask != null) {
            mCurrentTask.toString(stringBuilder);
        } else {
            stringBuilder.append("null");
        }
        stringBuilder.append(" }");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        toString(stringBuilder);
        return stringBuilder.toString();
    }

}
