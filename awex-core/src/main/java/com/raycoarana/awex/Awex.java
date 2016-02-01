package com.raycoarana.awex;

import com.raycoarana.awex.exceptions.AbsentValueException;

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
    private final AwexTaskQueue mWorkQueue;
    private final Set<Worker> mWorkers;
    private final int mMinThreads;
    private final int mMaxThreads;
    private final AtomicLong mThreadIdProvider = new AtomicLong();
    private final ScheduledExecutorService mMonitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService mCallbackExecutor = Executors.newSingleThreadExecutor();
    private AwexPromise mAbsentPromise;

    public Awex(UIThread uiThread, Logger logger, int minThreads, int maxThreads) {
        mUIThread = uiThread;
        mLogger = logger;
        mWorkQueue = new AwexTaskQueue();
        mWorkers = new HashSet<>(maxThreads);
        mMinThreads = minThreads;
        mMaxThreads = maxThreads;

        initializeThreads();
        initializeAbsentPromise();
    }

    private void initializeThreads() {
        for (int i = 0; i < mMinThreads; i++) {
            createNewWorker();
        }
    }

    private void initializeAbsentPromise() {
        mAbsentPromise = new AwexPromise<>(this);
        mAbsentPromise.reject(new AbsentValueException());
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

    public <T> Promise<T> submit(final Task<T> task) {
        synchronized (this) {
            task.initialize(this);
            task.markQueue();

            boolean isRealTimeTask = task.getPriority() == Task.PRIORITY_REAL_TIME;
            if (isRealTimeTask && (mWorkQueue.size() != 0 || mWorkQueue.waiters() == 0)) {
                new RealTimeWorker(mThreadIdProvider.incrementAndGet(), task, mLogger);
            } else {
                if (mWorkQueue.waiters() == 0 && mWorkers.size() < mMaxThreads) {
                    createNewWorker();
                }
                mWorkQueue.insert(task);
            }
        }
        return task.getPromise();
    }

    void submit(Runnable runnable) {
        mCallbackExecutor.submit(runnable);
    }

    public <T> void cancel(Task<T> task, boolean mayInterrupt) {
        synchronized (this) {
            task.softCancel();
            if (!mWorkQueue.remove(task) && mayInterrupt) {
                Worker worker = task.getWorker();
                if (worker != null) {
                    mWorkers.remove(worker);
                    worker.interrupt();
                }
            }
        }
    }

    /**
     * Creates a new promise that will be resolved only if all promises get resolved. If any of the
     * promises is rejected the created promise will be rejected.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that only will be resolve if all promises get resolved, otherwise it
     * will fail.
     */
    public <T> Promise<Collection<T>> allOf(Promise<T>... promises) {
        return allOf(Arrays.asList(promises));
    }

    /**
     * Creates a new promise that will be resolved only if all promises get resolved. If any of the
     * promises is rejected the created promise will be rejected.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that only will be resolve if all promises get resolved, otherwise it
     * will fail.
     */
    public <T> Promise<Collection<T>> allOf(Collection<Promise<T>> promises) {
        return new AllOfPromise<>(this, promises);
    }

    /**
     * Creates a new promise that will be resolved if any promise get resolved.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that will be resolve if any promise get resolved, otherwise it
     * will fail.
     */
    public <T> Promise<T> anyOf(Promise<T>... promises) {
        return anyOf(Arrays.asList(promises));
    }

    /**
     * Creates a new promise that will be resolved if any promise get resolved.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that will be resolve if any promise get resolved, otherwise it
     * will fail.
     */
    public <T> Promise<T> anyOf(Collection<Promise<T>> promises) {
        return new AnyOfPromise<>(this, promises);
    }

    /**
     * Creates a new promise that will be resolved when all promises finishes its execution, that
     * is, get resolved or rejected.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that will be resolved when all promises finishes its execution.
     */
    public <T> Promise<MultipleResult<T>> afterAll(Promise<T>... promises) {
        return afterAll(Arrays.asList(promises));
    }

    /**
     * Creates a new promise that will be resolved when all promises finishes its execution, that
     * is, get resolved or rejected.
     *
     * @param promises
     * @param <T>      type of result of the promises
     * @return a new promise that will be resolved when all promises finishes its execution.
     */
    public <T> Promise<MultipleResult<T>> afterAll(Collection<Promise<T>> promises) {
        return new AfterAllPromise<>(this, promises);
    }

    /**
     * Creates an already resolved promise with the value passed as parameter
     *
     * @param value value to use to resolve the promise, in case that the value is null a rejected promise is returned
     * @param <T>   type of the result
     * @return a promise already resolved
     */
    @SuppressWarnings("unchecked")
    public <T> Promise<T> of(T value) {
        if (value == null) {
            return (Promise<T>) mAbsentPromise;
        } else {
            AwexPromise<T> promise = new AwexPromise<>(this);
            promise.resolve(value);
            return promise;
        }
    }

    public <T> AwexPromise<T> newAwexPromise() {
        return new AwexPromise<>(this);
    }

    /**
     * Returns an already rejected promise
     *
     * @param <T> type of result
     * @return a promise already rejected
     */
    @SuppressWarnings("unchecked")
    public <T> Promise<T> absent() {
        return (Promise<T>) mAbsentPromise;
    }

    int getNumberOfThreads() {
        return Runtime.getRuntime().availableProcessors();
    }
}