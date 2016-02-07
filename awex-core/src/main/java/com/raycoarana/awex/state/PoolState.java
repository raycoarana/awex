package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;

public interface PoolState {
    QueueState getQueue(int queueId);

    Task getEqualTaskInQueue(Task task);
}
