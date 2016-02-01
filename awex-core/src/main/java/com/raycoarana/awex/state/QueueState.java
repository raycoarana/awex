package com.raycoarana.awex.state;

import java.util.Map;

public class QueueState {

    /**
     * Total number of tasks in the queue
     */
    public final int enqueue;

    /**
     * Total number of workers waiting for work
     */
    public final int waiters;

    /**
     * Info with workers state
     */
    public final Map<Long, WorkerState> workers;

    public QueueState(int enqueue, int waiters, Map<Long, WorkerState> workers) {
        this.enqueue = enqueue;
        this.waiters = waiters;
        this.workers = workers;
    }

}