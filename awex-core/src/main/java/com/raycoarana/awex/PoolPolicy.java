package com.raycoarana.awex;

import com.raycoarana.awex.state.PoolState;

public abstract class PoolPolicy {

    private PoolManager mPoolManager;

    public void initialize(PoolManager poolManager) {
        mPoolManager = poolManager;
        onStartUp();
    }

    /**
     * Creates a new queue in the pool
     *
     * @param queueId id of the queue
     */
    public void createQueue(int queueId) {
        mPoolManager.createQueue(queueId);
    }

    /**
     * Removes the queue from the pool, any worker associated with that queue will be interrupted
     *
     * @param queueId id of the queue
     */
    public void removeQueue(int queueId) {
        mPoolManager.removeQueue(queueId);
    }

    /**
     * Queue the task in the queue with the provided id
     *
     * @param queueId id of the destination queue
     * @param task task to be queue
     */
    public void queueTask(int queueId, Task task) {
        mPoolManager.queueTask(queueId, task);
    }

    /**
     * Merges a not queue task with an already queue (and maybe even already being executed) task,
     * so any result, error or progress from the task in the queue will be redirected to the
     * merged task. So any callback attached to the promise of the merged task will receive the
     * same result of the task in queue without having to execute again the task.
     *
     * @param taskInQueue task that is already queue and/or executing
     * @param taskToMerge task to be merged, must not be queue in any queue before executing this
     *                    method
     */
    public void mergeTask(Task taskInQueue, Task taskToMerge) {
        mPoolManager.mergeTask(taskInQueue, taskToMerge);
    }

    /**
     * Create a temporal worker and executes the task in it immediately, after that the worker is
     * aborted and the thread killed.
     *
     * @param task task to execute immediately
     */
    public void executeImmediately(Task task) {
        mPoolManager.executeImmediately(task);
    }

    /**
     * Creates a new worker and associate it to the queue with the provided id. The worker will
     * start executing tasks in that queue as soon as this method is executed and even before
     * this method returns.
     *
     * The priority of the thread is setup at worker start-up by ThreadHelper class used to
     * initialize Awex. The priority value will depend on the implementation of
     * @see ThreadHelper#setUpPriorityToCurrentThread as it will vary depending of the system.
     *
     * @param queueId id of the queue where the worker will be listen for tasks to execute
     * @param priority priority of the worker thread, @see com.raycoarana.awex.ThreadHelper#setUpPriorityToCurrentThread
     * @return id of the created worker
     */
    public int createWorker(int queueId, int priority) {
        return mPoolManager.createWorker(queueId, priority);
    }

    /**
     * Removes a worker with the specified id from the queue. If the worker is currently
     * executing a task, it will finish the task execution before it dies.
     *
     * @param queueId id of the queue where the worker belongs
     * @param workerId id of the worker to remove from the queue
     */
    public void removeWorker(int queueId, int workerId) {
        removeWorker(queueId, workerId, false);
    }

    /**
     * Removes a worker with the specified id from the queue. If the worker is currently
     * executing a task, it will be interrupted or not based on the interrupt parameter.
     *
     * @param queueId id of the queue where the worker belongs
     * @param workerId id of the worker to remove from the queue
     * @param interrupt indicates if the current executing task should be interrupted
     */
    public void removeWorker(int queueId, int workerId, boolean interrupt) {
        mPoolManager.removeWorker(queueId, workerId, interrupt);
    }

    /**
     * The pool is starting-up, its time to create the basic work queues and workers
     */
    public abstract void onStartUp();

    /**
     * A new task is added to the pool. Policy must manage where the task will be executed,
     * could create new queues (@see createQueue), create new worker for that queues
     * (@see createWorker) and queue the task in any of the existing queues (@see queueTask).
     * Even if the task should be executed in real time, the policy could create an specialized
     * worker that will execute the work immediately (@see executeImmediately).
     *
     * @param poolState thread pool state
     * @param task task in queue that timed out
     */
    public abstract void onTaskAdded(PoolState poolState, Task task);

    /**
     * Event dispatched when a task finishes. Policy could manage the state of the pool, reducing
     * the resources consumed by removing workers (@see removeWorker) or removing queues (@see remove
     *
     * @param poolState thread pool state
     * @param task task in queue that timed out
     */
    public abstract void onTaskFinished(PoolState poolState, Task task);

    /**
     * Event dispatched when a task in a queue timeout. The task is removed from the queue and
     * it will not be executed again. It's policy responsibility to reschedule or cancel the task
     *
     * @param poolState thread pool state
     * @param task task in queue that timed out
     */
    public abstract void onTaskQueueTimeout(PoolState poolState, Task task);

    /**
     * Event dispatched when a task execution timeout. The Task is not aborted or cancelled,
     * is policy responsibility to manage what happens with this task and with the worker that is
     * executing it.
     *
     * @param poolState thread pool state
     * @param task task in execution that timed out
     */
    public abstract void onTaskExecutionTimeout(PoolState poolState, Task task);

}