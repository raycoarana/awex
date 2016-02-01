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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class Awex implements PoolManager, WorkerListener {

    private final UIThread mUIThread;
    private final Logger mLogger;
    private final AtomicLong mWorkIdProvider = new AtomicLong();
    private final HashMap<Integer, AwexTaskQueue> mTaskQueueMap;
    private final HashMap<Integer, Map<Long, Worker>> mWorkers;
    private final PoolPolicy mPoolPolicy;
    private final AtomicLong mThreadIdProvider = new AtomicLong();
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

        mPoolPolicy.initialize(this);
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
            QueueState queueState = new QueueState(queue.size(),
                    queue.waiters(),
                    extractWorkersInfo(queue.getId()));
            queueStates.put(queue.getId(), queueState);
        }
        return queueStates;
    }

    private Map<Long, WorkerState> extractWorkersInfo(int queueId) {
        Set<Worker> workers = new HashSet<>(mWorkers.get(queueId).values());
        Map<Long, WorkerState> stateMap = new HashMap<>();
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
                        mWorkers.get(taskQueue.getId()).remove(worker);
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

    @Override
    public void createQueue(int queueId) {
        if (mTaskQueueMap.containsKey(queueId)) {
            throw new IllegalStateException("Trying to create a queue with an id that already exists");
        }

        mTaskQueueMap.put(queueId, new AwexTaskQueue(queueId));
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
    public long createWorker(int queueId) {
        AwexTaskQueue taskQueue = mTaskQueueMap.get(queueId);
        Map<Long, Worker> workersOfQueue = mWorkers.get(queueId);
        if (workersOfQueue == null) {
            workersOfQueue = new HashMap<>();
            mWorkers.put(queueId, workersOfQueue);
        }

        long id = mThreadIdProvider.incrementAndGet();
        workersOfQueue.put(id, new Worker(id, taskQueue, mLogger, this));
        return id;
    }

    @Override
    public void removeWorker(int queueId, long workerId) {
        removeWorker(queueId, workerId, false);
    }

    @Override
    public void removeWorker(int queueId, long workerId, boolean shouldInterrupt) {
        Worker worker = mWorkers.get(queueId).remove(workerId);
        if (worker != null) {
            if (shouldInterrupt) {
                worker.interrupt();
            } else {
                worker.die();
            }
        }
    }

    @Override
    public void onTaskFinished(Task task) {
        mPoolPolicy.onTaskFinished(extractPoolState(), task);
    }

    void schedule(TimerTask timerTask, int timeout) {
        if (timeout > 0) {
            mTimer.schedule(timerTask, timeout);
        }
    }

    public <T> void onTaskQueueTimeout(Task<T> task) {
        mPoolPolicy.onTaskQueueTimeout(extractPoolState(), task);
    }

    public <T> void onTaskExecutionTimeout(Task<T> task) {
        mPoolPolicy.onTaskExecutionTimeout(extractPoolState(), task);
    }
}