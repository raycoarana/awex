package com.raycoarana.awex.state;

import com.raycoarana.awex.util.Map;
import com.raycoarana.awex.util.ObjectPool;

public class QueueStateImpl implements QueueState {

    private int mId;
    private int mEnqueue;
    private int mWaiters;
    private final Map<Integer, WorkerStateImpl> mWorkers = Map.Provider.get();

    private final static ObjectPool<QueueStateImpl> sObjectPool = new ObjectPool<>(30);

    public static QueueStateImpl get(int id, int enqueue, int waiters) {
        QueueStateImpl queueState;
        synchronized (sObjectPool) {
            queueState = sObjectPool.acquire();
            if (queueState == null) {
                queueState = new QueueStateImpl();
            }
            queueState.mId = id;
            queueState.mEnqueue = enqueue;
            queueState.mWaiters = waiters;
        }
        return queueState;
    }

    private QueueStateImpl() {
    }

    /**
     * Id of the queue
     */
    @Override
    public int getId() {
        return mId;
    }

    /**
     * Total number of tasks in the queue
     */
    @Override
    public int getEnqueue() {
        return mEnqueue;
    }

    /**
     * Total number of mWorkers waiting for work
     */
    @Override
    public int getWaiters() {
        return mWaiters;
    }

    @Override
    public int numberOfWorkers() {
        return mWorkers.size();
    }

    public void addWorker(int id, WorkerStateImpl workerState) {
        mWorkers.put(id, workerState);
    }

    public void recycle() {
        for (WorkerStateImpl workerState : mWorkers.values()) {
            workerState.recycle();
        }
        mWorkers.clear();
        synchronized (sObjectPool) {
            sObjectPool.release(this);
        }
    }

    public void toString(StringBuilder stringBuilder) {
        stringBuilder.append("{/*Queue*/ id: ")
                .append(mId)
                .append(", enqueue: ")
                .append(mEnqueue)
                .append(", waiters: ")
                .append(mWaiters)
                .append(", workers: [");
        for (WorkerStateImpl workerState : mWorkers.values()) {
            workerState.toString(stringBuilder);
            stringBuilder.append(", ");
        }
        if (mWorkers.size() > 0) {
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
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