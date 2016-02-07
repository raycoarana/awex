package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PoolStateImpl implements PoolState {

    private final Map<Integer, QueueStateImpl> mQueueStateMap = new HashMap<>();
    private final Map<Task, Task> mTasks = new HashMap<>();

    private PoolStateImpl() {
    }

    private final static Queue<PoolStateImpl> sObjectPool = new LinkedList<>();

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

    public void addTasks(Set<Task> tasks) {
        for (Task task : (tasks)) {
            mTasks.put(task, task);
        }
    }

    public void recycle() {
        synchronized (sObjectPool) {
            for (QueueStateImpl queueState : mQueueStateMap.values()) {
                queueState.recycle();
            }
            mQueueStateMap.clear();
            mTasks.clear();
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