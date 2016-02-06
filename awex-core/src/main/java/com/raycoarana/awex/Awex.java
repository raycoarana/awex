package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.exceptions.AbsentValueException;
import com.raycoarana.awex.state.PoolState;
import com.raycoarana.awex.state.QueueState;
import com.raycoarana.awex.state.WorkerState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Awex {

    private final UIThread mUIThread;
    private final Logger mLogger;
    private final AtomicLong mWorkIdProvider = new AtomicLong();
    private final HashMap<Integer, AwexTaskQueue> mTaskQueueMap;
    private final HashMap<Integer, Map<Integer, Worker>> mWorkers;
    private final PoolPolicy mPoolPolicy;
    private final AtomicInteger mThreadIdProvider = new AtomicInteger();
    private final ExecutorService mCallbackExecutor = Executors.newSingleThreadExecutor();
    private final Timer mTimer;
    private AwexPromise mAbsentPromise;

    public Awex(UIThread uiThread, Logger logger) {
        this(uiThread, logger, PoolPolicy.DEFAULT);
    }

    public Awex(UIThread uiThread, Logger logger, PoolPolicy poolPolicy) {
        mUIThread = uiThread;
        mLogger = logger;
        mTaskQueueMap = new HashMap<>();
        mWorkers = new HashMap<>();
        mPoolPolicy = poolPolicy;
        mTimer = new Timer();

        initializeAbsentPromise();

        mPoolPolicy.initialize(new PoolManagerImpl());
    }

    private void initializeAbsentPromise() {
        mAbsentPromise = new AwexPromise<>(this);
        mAbsentPromise.reject(new AbsentValueException());
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
            PoolState poolState = extractPoolState();
            mPoolPolicy.onTaskAdded(poolState, task);
        }
        return task.getPromise();
    }

    private PoolState extractPoolState() {
        return new PoolState(extractQueueState());
    }

    private Map<Integer, QueueState> extractQueueState() {
        List<AwexTaskQueue> queues = new ArrayList<>(mTaskQueueMap.values());
        Map<Integer, QueueState> queueStates = new HashMap<>(queues.size());
        for (AwexTaskQueue queue : queues) {
            QueueState queueState = new QueueState(queue.getId(),
                    queue.size(),
                    queue.waiters(),
                    extractWorkersInfo(queue.getId()));
            queueStates.put(queue.getId(), queueState);
        }
        return queueStates;
    }

    private Map<Integer, WorkerState> extractWorkersInfo(int queueId) {
        Set<Worker> workers = new HashSet<>(mWorkers.get(queueId).values());
        Map<Integer, WorkerState> stateMap = new HashMap<>();
        for (Worker worker : workers) {
            stateMap.put(worker.getId(), worker.takeState());
        }

        return stateMap;
    }

    void submit(Runnable runnable) {
        mCallbackExecutor.submit(runnable);
    }

    public <T> void cancel(Task<T> task, boolean mayInterrupt) {
        synchronized (this) {
            task.softCancel();
            AwexTaskQueue taskQueue = task.getQueue();
            if (taskQueue != null) {
                if (!taskQueue.remove(task) && mayInterrupt) {
                    Worker worker = task.getWorker();
                    if (worker != null) {
                        mWorkers.get(taskQueue.getId()).remove(worker.getId());
                        worker.interrupt();
                    }
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
    @SafeVarargs
    public final <T> Promise<Collection<T>> allOf(Promise<T>... promises) {
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
    @SafeVarargs
    public final <T> Promise<T> anyOf(Promise<T>... promises) {
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
    @SafeVarargs
    public final <T> Promise<MultipleResult<T>> afterAll(Promise<T>... promises) {
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

    void schedule(TimerTask timerTask, int timeout) {
        if (timeout > 0) {
            mTimer.schedule(timerTask, timeout);
        }
    }

    <T> void onTaskQueueTimeout(Task<T> task) {
        mPoolPolicy.onTaskQueueTimeout(extractPoolState(), task);
    }

    <T> void onTaskExecutionTimeout(Task<T> task) {
        mPoolPolicy.onTaskExecutionTimeout(extractPoolState(), task);
    }

    private final WorkerListener mWorkerListener = new WorkerListener() {

        @Override
        public void onTaskFinished(Task task) {
            mPoolPolicy.onTaskFinished(extractPoolState(), task);
        }

    };

    private class PoolManagerImpl implements PoolManager {

        @Override
        public synchronized void createQueue(int queueId) {
            if (mTaskQueueMap.containsKey(queueId)) {
                throw new IllegalStateException("Trying to create a queue with an id that already exists");
            }

            mTaskQueueMap.put(queueId, new AwexTaskQueue(queueId));
        }

        @Override
        public synchronized void removeQueue(int queueId) {
            AwexTaskQueue awexTaskQueue = mTaskQueueMap.remove(queueId);
            Map<Integer, Worker> workersOfQueue = mWorkers.remove(queueId);
            for (Worker worker : workersOfQueue.values()) {
                worker.die();
            }
            awexTaskQueue.destroy();
            for (Worker worker : workersOfQueue.values()) {
                worker.interrupt();
            }
        }

        @Override
        public void executeImmediately(Task task) {
            task.markQueue(null);
            new RealTimeWorker(mThreadIdProvider.incrementAndGet(), task, mLogger);
        }

        @Override
        public void queueTask(int queueId, Task task) {
            AwexTaskQueue taskQueue = mTaskQueueMap.get(queueId);
            task.markQueue(taskQueue);
            taskQueue.insert(task);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void mergeTask(Task taskInQueue, final Task taskToMerge) {
            if (taskInQueue.getState() <= Task.STATE_NOT_QUEUE) {
                throw new IllegalStateException("Task not queued");
            }
            if (taskToMerge.getState() != Task.STATE_NOT_QUEUE) {
                throw new IllegalStateException("Task already queue");
            }

            taskToMerge.markQueue(null);
            final AwexPromise promiseToMerge = (AwexPromise) taskToMerge.getPromise();
            taskInQueue.getPromise().done(new DoneCallback() {
                @Override
                public void onDone(Object result) {
                    promiseToMerge.resolve(result);
                }
            }).fail(new FailCallback() {
                @Override
                public void onFail(Exception exception) {
                    promiseToMerge.reject(exception);
                }
            }).progress(new ProgressCallback() {
                @Override
                public void onProgress(float progress) {
                    promiseToMerge.notifyProgress(progress);
                }
            }).cancel(new CancelCallback() {
                @Override
                public void onCancel() {
                    promiseToMerge.cancelTask();
                }
            });
        }

        @Override
        public synchronized long createWorker(int queueId) {
            AwexTaskQueue taskQueue = mTaskQueueMap.get(queueId);
            Map<Integer, Worker> workersOfQueue = mWorkers.get(queueId);
            if (workersOfQueue == null) {
                workersOfQueue = new HashMap<>();
                mWorkers.put(queueId, workersOfQueue);
            }

            int id = mThreadIdProvider.incrementAndGet();
            workersOfQueue.put(id, new Worker(id, taskQueue, mLogger, mWorkerListener));
            return id;
        }

        @Override
        public synchronized void removeWorker(int queueId, int workerId) {
            removeWorker(queueId, workerId, false);
        }

        @Override
        public synchronized void removeWorker(int queueId, int workerId, boolean shouldInterrupt) {
            Worker worker = mWorkers.get(queueId).remove(workerId);
            if (worker != null) {
                if (shouldInterrupt) {
                    worker.interrupt();
                } else {
                    worker.die();
                }
            }
        }

    }

}