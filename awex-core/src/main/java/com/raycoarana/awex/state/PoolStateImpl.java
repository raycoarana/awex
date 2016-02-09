package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;
import com.raycoarana.awex.util.Map;

import java.util.ArrayDeque;
import java.util.Queue;

public class PoolStateImpl implements PoolState {

    private final Map<Integer, QueueStateImpl> mQueueStateMap = Map.Provider.get();
    private Map<Task, Task> mTasks;

    private PoolStateImpl() {
    }

    private final static Queue<PoolStateImpl> sObjectPool = new ArrayDeque<>(4);

    public static PoolStateImpl get() {
        PoolStateImpl poolState;
        synchronized (sObjectPool) {
            if (sObjectPool.size() > 0) {
                poolState = sObjectPool.poll();
            } else {
                poolState = new PoolStateImpl();
            }
        }
        return poolState;
    }

    @Override
    public QueueState getQueue(int queueId) {
        return mQueueStateMap.get(queueId);
    }

    public void addQueue(int queueId, QueueStateImpl queueState) {
        mQueueStateMap.put(queueId, queueState);
    }

    public void setTasks(Map<Task, Task> tasks) {
        mTasks = tasks;
    }

    public void recycle() {
        synchronized (sObjectPool) {
            for (QueueStateImpl queueState : mQueueStateMap.values()) {
                queueState.recycle();
            }
            mQueueStateMap.clear();
            sObjectPool.add(this);
        }
    }

    /**
     * Search for a task that has the same type and hash code in the queue. You should override
     *
     * @param task original task used to search a task that is equal
     * @return a task that is equal the provided task or null if no task is found
     * @see Task#equals(Object) and @see Task#hashCode to customize the behavior of locating
     * similar tasks
     */
    @Override
    public Task getEqualTaskInQueue(Task task) {
        return mTasks.get(task);
    }

    private void toString(StringBuilder stringBuilder) {
        stringBuilder.append("[ ");
        for (QueueStateImpl queueState : mQueueStateMap.values()) {
            queueState.toString(stringBuilder);
            stringBuilder.append(", ");
        }
        if (mQueueStateMap.size() > 0) {
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        }
        stringBuilder.append(" ]");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        toString(stringBuilder);
        return stringBuilder.toString();
    }
}