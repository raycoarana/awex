package com.raycoarana.awex.state;

import java.util.Collections;
import java.util.Map;

public class PoolState {

    private final Map<Integer, QueueState> mQueueStateMap;

    public PoolState(Map<Integer, QueueState> queueStateMap) {
        mQueueStateMap = Collections.unmodifiableMap(queueStateMap);
    }

    public QueueState getQueue(int queueId) {
        return mQueueStateMap.get(queueId);
    }

    public void toString(StringBuilder stringBuilder) {
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