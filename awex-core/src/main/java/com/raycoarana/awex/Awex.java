package com.raycoarana.awex;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class Awex {

    private final UIThread mUIThread;
    private final Logger mLogger;
    private final AtomicLong mWorkIdProvider = new AtomicLong();
    private final AwexWorkQueue mWorkQueue;
    private final Set<Worker> mWorkers;
    private final int mMinThreads;
    private final int mMaxThreads;
    private final AtomicLong mThreadIdProvider = new AtomicLong();
    private final ScheduledExecutorService mMonitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService mCallbackExecutor = Executors.newSingleThreadExecutor();

    public Awex(UIThread uiThread, Logger logger, int minThreads, int maxThreads) {
        mUIThread = uiThread;
        mLogger = logger;
        mWorkQueue = new AwexWorkQueue();
        mWorkers = new HashSet<>(maxThreads);
        mMinThreads = minThreads;
        mMaxThreads = maxThreads;

        initializeThreads();
    }

    private void initializeThreads() {
        for (int i = 0; i < mMinThreads; i++) {
            createNewWorker();
        }
    }

    private void createNewWorker() {
        mWorkers.add(new Worker(mThreadIdProvider.incrementAndGet(), mWorkQueue, mLogger));
    }

    UIThread provideUIThread() {
        return mUIThread;
    }

    Logger provideLogger() {
        return mLogger;
    }

    long provideWorkId() {
        return mWorkIdProvider.incrementAndGet();
    }

    public <T> Promise<T> submit(final Work<T> work) {
        synchronized (this) {
            work.initialize(this);
            work.markQueue();
            if (mWorkQueue.waiters() == 0 && mWorkers.size() < mMaxThreads) {
                createNewWorker();
            }
            mWorkQueue.insert(work);
        }
        return work.getPromise();
    }

    void submit(Runnable runnable) {
        mCallbackExecutor.submit(runnable);
    }

    public <T> void cancel(Work<T> work) {
        work.softCancel();
        //TODO: check in some time if softCancel do the cancel and if not do hardCancel calling interrupt on the thread
        // that will sacrifice the thread and we will need to recreate a new one...
    }

    /**
     * Creates a new promise that will be resolved only if all promises get resolved. If any of the
     * promises is rejected the created promise will be rejected.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that only will be resolve if all promises get resolved, otherwise if
     * will fail.
     */
    public <T> Promise<Collection<T>> allOf(Promise<T>... promises) {
        return allOf(Arrays.asList(promises));
    }

    public <T> Promise<Collection<T>> allOf(Collection<Promise<T>> promises) {
        return new AllOfPromise<>(this, promises);
    }

    public <T> Promise<T> anyOf(Promise<T>... promises) {
        return anyOf(Arrays.asList(promises));
    }

    public <T> Promise<T> anyOf(Collection<Promise<T>> promises) {
        return new AnyOfPromise<>(this, promises);
    }

    public <T> Promise<MultipleResult<T>> afterAll(Promise<T>... promises) {
        return afterAll(Arrays.asList(promises));
    }

    public <T> Promise<MultipleResult<T>> afterAll(Collection<Promise<T>> promises) {
        return new AfterAllPromise<>(this, promises);
    }
}