package com.raycoarana.awex;

import com.raycoarana.awex.state.PoolState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

public class PoolManagerTest {

    private static final Integer SOME_VALUE = 42;
    private static final Integer SOME_OTHER_VALUE = 24;
    private static final int FIRST_QUEUE = 1;
    private static final int SECOND_QUEUE = 2;

    @Mock
    private UIThread mUIThread;

    private Logger mLogger = new ConsoleLogger();

    private Awex mAwex;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCreateAndRemoveQueuesAndWorkers() throws Exception {
        final Task<Integer, Void> firstTask = new Task<Integer, Void>() {
            @Override
            protected Integer run() throws InterruptedException {
                return SOME_VALUE;
            }
        };
        Task<Integer, Void> secondTask = new Task<Integer, Void>() {
            @Override
            protected Integer run() throws InterruptedException {
                return SOME_OTHER_VALUE;
            }
        };

        setUpAwex(new PoolPolicy() {
            @Override
            public void onStartUp() {

            }

            @Override
            public void onTaskAdded(PoolState poolState, Task task) {
                mLogger.v(poolState.toString());
                if (firstTask == task) {
                    createQueue(FIRST_QUEUE);
                    createWorker(FIRST_QUEUE);
                    queueTask(FIRST_QUEUE, task);
                } else {
                    createQueue(SECOND_QUEUE);
                    createWorker(SECOND_QUEUE);
                    queueTask(SECOND_QUEUE, task);
                }
            }

            @Override
            public void onTaskFinished(PoolState poolState, Task task) {
                mLogger.v(poolState.toString());

                if (task == firstTask) {
                    removeQueue(FIRST_QUEUE);
                } else {
                    removeQueue(SECOND_QUEUE);
                }
            }

            @Override
            public void onTaskQueueTimeout(PoolState poolState, Task task) {

            }

            @Override
            public void onTaskExecutionTimeout(PoolState poolState, Task task) {

            }
        });

        assertEquals(SOME_VALUE, mAwex.submit(firstTask).getResult());
        assertEquals(SOME_OTHER_VALUE, mAwex.submit(secondTask).getResult());
        sleep();
        assertEquals("[  ]", mAwex.toString());
    }

    class StaticValueTask extends Task<Integer, Void> {

        private final int mValueToReturn;

        public StaticValueTask(int valueToReturn) {
            mValueToReturn = valueToReturn;
        }

        @Override
        protected Integer run() throws InterruptedException {
            sleep();
            return mValueToReturn;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Task && hashCode() == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    @Test
    public void shouldMergeTaskResult() throws Exception {
        final Task<Integer, Void> firstTask = new StaticValueTask(SOME_VALUE);
        Task<Integer, Void> secondTask = new StaticValueTask(SOME_OTHER_VALUE);

        setUpAwex(new PoolPolicy() {
            @Override
            public void onStartUp() {
                createQueue(FIRST_QUEUE);
                createWorker(FIRST_QUEUE);
            }

            @Override
            public void onTaskAdded(PoolState poolState, Task task) {
                mLogger.v(poolState.toString());
                Task taskInQueue = poolState.getEqualTaskInQueue(task);
                if (taskInQueue == null) {
                    queueTask(FIRST_QUEUE, task);
                } else {
                    mergeTask(taskInQueue, task);
                }
            }

            @Override
            public void onTaskFinished(PoolState poolState, Task task) {
                mLogger.v(poolState.toString());

                if (task == firstTask) {
                    removeQueue(FIRST_QUEUE);
                } else {
                    removeQueue(SECOND_QUEUE);
                }
            }

            @Override
            public void onTaskQueueTimeout(PoolState poolState, Task task) {

            }

            @Override
            public void onTaskExecutionTimeout(PoolState poolState, Task task) {

            }
        });

        assertEquals(SOME_VALUE, mAwex.submit(firstTask).getResult());
        assertEquals(SOME_VALUE, mAwex.submit(secondTask).getResult());
        sleep();
        assertEquals("[  ]", mAwex.toString());
    }

    @Test
    public void shouldRemoveWorker() throws Exception {
        final Task<Integer, Void> firstTask = new StaticValueTask(SOME_VALUE);
        Task<Integer, Void> secondTask = new StaticValueTask(SOME_OTHER_VALUE);

        setUpAwex(new PoolPolicy() {
            public int secondWorkerId;

            @Override
            public void onStartUp() {
                createQueue(FIRST_QUEUE);
                createWorker(FIRST_QUEUE);
            }

            @Override
            public void onTaskAdded(PoolState poolState, Task task) {
                assertEquals(1, poolState.getQueue(FIRST_QUEUE).workers.size());
                secondWorkerId = createWorker(FIRST_QUEUE);
                queueTask(FIRST_QUEUE, task);
            }

            @Override
            public void onTaskFinished(PoolState poolState, Task task) {
                assertEquals(2, poolState.getQueue(FIRST_QUEUE).workers.size());
                removeWorker(FIRST_QUEUE, secondWorkerId);
            }

            @Override
            public void onTaskQueueTimeout(PoolState poolState, Task task) {

            }

            @Override
            public void onTaskExecutionTimeout(PoolState poolState, Task task) {

            }
        });

        assertEquals(SOME_VALUE, mAwex.submit(firstTask).getResult());
        sleep();
        assertEquals(SOME_OTHER_VALUE, mAwex.submit(secondTask).getResult());
    }

    private void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setUpAwex(PoolPolicy poolPolicy) {
        mAwex = new Awex(mUIThread, new ConsoleLogger(), poolPolicy);
    }

}