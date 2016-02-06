package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;

import java.util.Collections;
import java.util.Map;

public class PoolState {

    private final Map<Integer, QueueState> mQueueStateMap;
    private final Map<Task, Task> mTasks;

    public PoolState(Map<Integer, QueueState> queueStateMap, Map<Task, Task> tasks) {
        mQueueStateMap = Collections.unmodifiableMap(queueStateMap);
        mTasks = tasks;
    }

    public QueueState getQueue(int queueId) {
        return mQueueStateMap.get(queueId);
    }

    /**
     * Search for a task that has the same type and hash code in the queue. You should override
     * @see Task#equals(Object) and @see Task#hashCode to customize the behavior of locating
     * similar tasks
     *
     * @param task original task used to search a task that is equal
     * @return a task that is equal the provided task or null if no task is found
     */
    public Task getEqualTaskInQueue(Task task) {
        return mTasks.get(task);
    }

    private void toString(StringBuilder stringBuilder) {
        stringBuilder.append("[ ");
        for (QueueState queueState : mQueueStateMap.values()) {
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