package com.raycoarana.awex;

import com.raycoarana.awex.policy.LinearWithRealTimePriority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AwexTest {

    private static final Integer SOME_VALUE = 42;

    @Mock
    private UIThread mUIThread;
    @Mock
    private Logger mLogger;

    private Awex mAwex;
    private Promise<Integer> mTaskPromise;
    private boolean mExecutionFlag;
    private Integer mResult;
    private ArrayList<Long> mResultCollection;
    private ArrayList<Long> mExceptedCollection;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldExecuteSimpleTask() throws Exception {
        setUpAwex();

        mTaskPromise = mAwex.submit(new Task<Integer>() {
            @Override
            protected Integer run() throws InterruptedException {
                return SOME_VALUE;
            }
        });

        assertEquals(SOME_VALUE, mTaskPromise.getResult());
    }

    @Test
    public void shouldExecuteSimpleVoidTask() throws Exception {
        setUpAwex();

        Promise<Void> promise = mAwex.submit(new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                mResult = SOME_VALUE;
            }
        });

        promise.getResult(); //Wait to finish
        assertEquals(SOME_VALUE, mResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldExecuteAndRejectPromiseOfFailingTask() throws Exception {
        setUpAwex();

        mTaskPromise = mAwex.submit(new Task<Integer>() {
            @Override
            protected Integer run() throws InterruptedException {
                throw new IllegalArgumentException("Argument not valid!");
            }
        });

        mTaskPromise.getResult();
    }

    @Test
    public void shouldCreateAnAlreadyResolvedPromise() throws Exception {
        setUpAwex();

        Promise<Integer> promise = mAwex.of(SOME_VALUE);

        assertTrue(promise.isResolved());
        assertEquals(SOME_VALUE, promise.getResult());
    }

    @Test
    public void shouldCreateARejectedPromiseWhenCreatedWithNullValue() {
        setUpAwex();

        Promise<Integer> promise = mAwex.of(null);

        assertTrue(promise.isRejected());
    }

    @Test
    public void shouldCreateARejectedPromise() {
        setUpAwex();

        Promise<Integer> promise = mAwex.absent();

        assertTrue(promise.isRejected());
    }

    @Test
    public void shouldInterruptTaskOnCancel() {
        setUpAwex();

        final Semaphore workIsRunning = new Semaphore(0);
        Promise<Integer> promise = mAwex.submit(new Task<Integer>() {

            @Override
            protected Integer run() throws InterruptedException {
                workIsRunning.release();
                Thread.sleep(10000);
                return null;
            }
        });

        workIsRunning.acquireUninterruptibly();
        promise.cancelTask(true);

        assertEquals(Promise.STATE_CANCELLED, promise.getState());
        assertFalse(mExecutionFlag);
    }

    @Test(timeout = 5000)
    public void shouldBeMarkedAsCancelledInterruptingGracefullyTheTaskOnCancel() {
        setUpAwex();

        final Semaphore workIsRunning = new Semaphore(0);
        mTaskPromise = mAwex.submit(new Task<Integer>() {

            @Override
            protected Integer run() throws InterruptedException {
                workIsRunning.release();
                while(!isCancelled()) {
                    mAwex.provideLogger().v("I'm doing something");
                }
                mExecutionFlag = true;
                workIsRunning.release();
                return null;
            }
        });

        workIsRunning.acquireUninterruptibly();
        mTaskPromise.cancelTask(true);

        assertEquals(Promise.STATE_CANCELLED, mTaskPromise.getState());
        workIsRunning.acquireUninterruptibly();
        assertTrue(mExecutionFlag);
    }

    @Test
    public void shouldExecuteFirstlyTaskWithMorePriority() throws Exception {
        setUpAwex();

        mExceptedCollection = new ArrayList<>();
        mResultCollection = new ArrayList<>();
        final Semaphore blocker = new Semaphore(0);
        mAwex.submit(new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                blocker.acquireUninterruptibly();
            }
        });

        VoidTask lowPriorityTask = new VoidTask(Task.PRIORITY_LOW) {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                mResultCollection.add(getId());
            }
        };
        mAwex.submit(lowPriorityTask);
        VoidTask highPriorityTask = new VoidTask(Task.PRIORITY_HIGH) {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                mResultCollection.add(getId());
            }
        };
        mAwex.submit(highPriorityTask);

        mExceptedCollection.add(highPriorityTask.getId());
        mExceptedCollection.add(lowPriorityTask.getId());

        blocker.release();
        lowPriorityTask.getPromise().getResult();
        highPriorityTask.getPromise().getResult();

        assertEquals(mExceptedCollection.get(0), mResultCollection.get(0));
        assertEquals(mExceptedCollection.get(1), mResultCollection.get(1));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToSubmitTwoTimesTheSameTask() {
        setUpAwex();

        VoidTask someTask = new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        };
        mAwex.submit(someTask);
        mAwex.submit(someTask);
    }

    @Test(timeout = 500)
    public void shouldLetToSubmitAgainTheSameTaskIfResetWhenFinished() throws Exception {
        setUpAwex();

        VoidTask someTask = new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        };
        mAwex.submit(someTask);
        someTask.getPromise().getResult();

        someTask.reset();

        mAwex.submit(someTask);
        someTask.getPromise().getResult();

        assertEquals(Promise.STATE_RESOLVED, someTask.getPromise().getState());
    }

    @Test(timeout = 1000)
    public void shouldExecuteAsRealTimeWhenAllThreadsBusy() throws Exception {
        setUpAwex();

        final Semaphore semaphore = new Semaphore(0);
        VoidTask someWaitingTask = new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                semaphore.acquire();
            }
        };

        VoidTask realTimeTask = new VoidTask(Task.PRIORITY_REAL_TIME) {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        };

        Promise<Void> waitingPromise = mAwex.submit(someWaitingTask);
        Promise<Void> realTimePromise = mAwex.submit(realTimeTask);

        realTimePromise.getResult();

        semaphore.release();
        waitingPromise.getResult();
    }

    @Test(timeout = 1000)
    public void shouldExecuteAsRealTimeWhenWorkQueueHasQueuedTasks() throws Exception {
        setUpAwex();

        final Semaphore semaphore = new Semaphore(0);
        VoidTask someWaitingTask = new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {
                semaphore.acquire();
            }
        };

        VoidTask realTimeTask = new VoidTask(Task.PRIORITY_REAL_TIME) {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        };

        Promise<Void> waitingPromise = mAwex.submit(someWaitingTask);
        mAwex.submit(new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        });
        Promise<Void> realTimePromise = mAwex.submit(realTimeTask);

        realTimePromise.getResult();

        semaphore.release();
        waitingPromise.getResult();
    }

    @Test
    public void shouldCreateAllOfPromise() {
        setUpAwex();

        Promise<Collection<Integer>> allOfPromise = mAwex.allOf(new AwexPromise<Integer>(mAwex),
                new AwexPromise<Integer>(mAwex));

        assertThat(allOfPromise, instanceOf(AllOfPromise.class));
    }

    @Test
    public void shouldCreateAnyOfPromise() {
        setUpAwex();

        Promise<Integer> anyOfPromise = mAwex.anyOf(new AwexPromise<Integer>(mAwex),
                new AwexPromise<Integer>(mAwex));

        assertThat(anyOfPromise, instanceOf(AnyOfPromise.class));
    }

    @Test
    public void shouldCreateAfterAllPromise() {
        setUpAwex();

        Promise<MultipleResult<Integer>> afterAllPromise = mAwex.afterAll(
                new AwexPromise<Integer>(mAwex),
                new AwexPromise<Integer>(mAwex));

        assertThat(afterAllPromise, instanceOf(AfterAllPromise.class));
    }

    @Test
    public void shouldAbortTaskInQueueIfTimeoutExpires() {
        setUpAwex();

        final Semaphore semaphore = new Semaphore(0);

        mAwex.submit(new Task<Integer>() {

            @Override
            protected Integer run() throws InterruptedException {
                semaphore.acquire();
                return null;
            }
        });

        mTaskPromise = mAwex.submit(new Task<Integer>(Task.PRIORITY_NORMAL, 500, 500) {
            @Override
            protected Integer run() throws InterruptedException {
                return SOME_VALUE;
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(mTaskPromise.isCancelled());

        semaphore.release();
    }

    @Test
    public void shouldAbortTaskInExecutionIfTimeoutExpires() {
        setUpAwex();

        final Semaphore semaphore = new Semaphore(0);

        mTaskPromise = mAwex.submit(new Task<Integer>(Task.PRIORITY_NORMAL, 500, 500) {
            @Override
            protected Integer run() throws InterruptedException {
                semaphore.acquire();
                return SOME_VALUE;
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(mTaskPromise.isCancelled());

        semaphore.release();
    }

    private void setUpAwex() {
        mAwex = new Awex(mUIThread, new ConsoleLogger(), new LinearWithRealTimePriority(1));
    }

}