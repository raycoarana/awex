package com.raycoarana.awex.state;

import java.util.Map;

public class PoolState {

    private final Map<Integer, QueueState> mQueueStateMap;

    public PoolState(Map<Integer, QueueState> queueStateMap) {
        mQueueStateMap = queueStateMap;
    }

    public QueueState getQueue(int queueId) {
        return mQueueStateMap.get(queueId);
    }

}