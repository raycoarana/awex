package com.raycoarana.awex;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class AwexWorkQueue {

    private static final int INITIAL_CAPACITY = 4;

    private final PriorityBlockingQueue<Work> mWorkQueue;
    private final AtomicInteger mWaitersCount = new AtomicInteger();

    public AwexWorkQueue() {
        mWorkQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY, new WorkPriorityComparator());
    }

    public void insert(Work work) {
        mWorkQueue.offer(work);
    }

    public Work take() throws InterruptedException {
        try {
            mWaitersCount.incrementAndGet();
            return mWorkQueue.take();
        } finally {
            mWaitersCount.decrementAndGet();
        }
    }

    public int waiters() {
        return mWaitersCount.get();
    }

}