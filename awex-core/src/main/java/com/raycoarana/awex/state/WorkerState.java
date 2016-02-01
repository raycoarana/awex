package com.raycoarana.awex.state;

import com.raycoarana.awex.Task;

import java.util.concurrent.locks.LockSupport;

public class WorkerState {

    public final State state;
    public final Task currentTask;
    public final long lastTimeActive;

    public WorkerState(State state, Task currentTask, long lastTimeActive) {
        this.state = state;
        this.currentTask = currentTask;
        this.lastTimeActive = lastTimeActive;
    }

    public enum State {
        /**
         * State for a worker which has not yet started.
         */
        NEW,

        /**
         * State for a runnable worker.  A worker in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         */
        RUNNABLE,

        /**
         * State for a worker blocked waiting for a monitor lock.
         * The worker in the blocked state is waiting for a monitor lock
         * to enter a synchronized block/method or
         * reenter a synchronized block/method after calling
         * {@link Object#wait() Object.wait}.
         */
        BLOCKED,

        /**
         * State for a waiting worker.
         * Worker is in the waiting state due to calling one of the
         * following methods:
         * <ul>
         * <li>{@link Object#wait() Object.wait} with no timeout</li>
         * <li>{@link Thread#join() Thread.join} with no timeout</li>
         * <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         * <p/>
         * <p>A worker in the waiting state is waiting for another thread to
         * perform a particular action.
         * <p/>
         * For example, the worker thread that has called <tt>Object.wait()</tt>
         * on an object is waiting for another thread to call
         * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on
         * that object. The worker thread that has called <tt>Thread.join()</tt>
         * is waiting for a specified thread to terminate.
         */
        WAITING,

        /**
         * State for a waiting worker with a specified waiting time.
         * The worker is in the timed waiting state due to calling one of
         * the following methods with a specified positive waiting time:
         * <ul>
         * <li>{@link Thread#sleep Thread.sleep}</li>
         * <li>{@link Object#wait(long) Object.wait} with timeout</li>
         * <li>{@link Thread#join(long) Thread.join} with timeout</li>
         * <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
         * <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
         * </ul>
         */
        TIMED_WAITING,

        /**
         * State for terminated worker. No more tasks will be executed by this worker.
         */
        TERMINATED,

        /**
         * State for a worker waiting for the next task.
         * Worker is in waiting for next task state when the worker is waiting for next
         * tasks to be queue.
         */
        WAITING_FOR_NEXT_TASK
    }

}
