package com.raycoarana.awex.policy;

import com.raycoarana.awex.PoolPolicy;
import com.raycoarana.awex.Task;
import com.raycoarana.awex.state.PoolState;
import com.raycoarana.awex.state.QueueState;

public class LinearWithRealTimePriority extends PoolPolicy {

    private static final int QUEUE_ID = 1;

    private final int mMaxThreads;

    public LinearWithRealTimePriority() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public LinearWithRealTimePriority(int maxThreads) {
        mMaxThreads = maxThreads;
    }

    @Override
    public void onStartUp() {
        createQueue(QUEUE_ID);
        createWorker(QUEUE_ID);
    }

    @Override
    public void onTaskAdded(PoolState poolState, Task task) {
        QueueState queueState = poolState.getQueue(QUEUE_ID);

        boolean isRealTimeTask = task.getPriority() == Task.PRIORITY_REAL_TIME;
        if (isRealTimeTask && (queueState.getEnqueue() != 0 || queueState.getWaiters() == 0)) {
            executeImmediately(task);
        } else {
            if (queueState.getWaiters() == 0 && queueState.numberOfWorkers() < mMaxThreads) {
                createWorker(QUEUE_ID);
            }
            queueTask(QUEUE_ID, task);
        }
    }

    @Override
    public void onTaskFinished(PoolState poolState, Task task) {

    }

    @Override
    public void onTaskQueueTimeout(PoolState poolState, Task task) {
        task.getPromise().cancelTask();
    }

    @Override
    public void onTaskExecutionTimeout(PoolState poolState, Task task) {
        task.getPromise().cancelTask();
    }
}