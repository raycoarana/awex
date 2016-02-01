package com.raycoarana.awex;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class AwexTaskQueue {

    private static final int INITIAL_CAPACITY = 4;

    private final PriorityBlockingQueue<Task> mTaskQueue;
    private final AtomicInteger mWaitersCount = new AtomicInteger();
    private int mId;

    public AwexTaskQueue(int id) {
        mId = id;
        mTaskQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY, new TaskPriorityComparator());
    }

    public synchronized Task take(Worker worker) throws InterruptedException {
        try {
            mWaitersCount.incrementAndGet();
            Task task =  mTaskQueue.take();
            task.setWorker(worker);
            return task;
        } finally {
            mWaitersCount.decrementAndGet();
        }
    }

    public void insert(Task task) {
        mTaskQueue.offer(task);
    }

    public synchronized <T> boolean remove(Task<T> task) {
        return mTaskQueue.remove(task);
    }

    public int waiters() {
        return mWaitersCount.get();
    }

    public int size() {
        return mTaskQueue.size();
    }

    public int getId() {
        return mId;
    }

}