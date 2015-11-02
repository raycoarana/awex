package com.raycoarana.awex;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class AwexTaskQueue {

    private static final int INITIAL_CAPACITY = 4;

    private final PriorityBlockingQueue<Task> mTaskQueue;
    private final AtomicInteger mWaitersCount = new AtomicInteger();

    public AwexTaskQueue() {
        mTaskQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY, new TaskPriorityComparator());
    }

    public void insert(Task task) {
        mTaskQueue.offer(task);
    }

    public Task take() throws InterruptedException {
        try {
            mWaitersCount.incrementAndGet();
            return mTaskQueue.take();
        } finally {
            mWaitersCount.decrementAndGet();
        }
    }

    public int waiters() {
        return mWaitersCount.get();
    }

}