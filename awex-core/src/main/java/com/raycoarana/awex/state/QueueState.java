package com.raycoarana.awex.state;

import java.util.Collections;
import java.util.Map;

public class QueueState {

    /**
     * Id of the queue
     */
    public final int id;

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
    public final Map<Integer, WorkerState> workers;

    public QueueState(int id, int enqueue, int waiters, Map<Integer, WorkerState> workers) {
        this.id = id;
        this.enqueue = enqueue;
        this.waiters = waiters;
        this.workers = Collections.unmodifiableMap(workers);
    }

    public void toString(StringBuilder stringBuilder) {
        stringBuilder.append("{/*Queue*/ id: ")
                .append(id)
                .append(", enqueue: ")
                .append(enqueue)
                .append(", waiters: ")
                .append(waiters)
                .append(", workers: [");
        for (WorkerState workerState : workers.values()) {
            workerState.toString(stringBuilder);
            stringBuilder.append(", ");
        }
        if (workers.size() > 0) {
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