package com.raycoarana.awex;

import com.raycoarana.awex.exceptions.EmptyTasksException;
import com.raycoarana.awex.policy.LinearWithRealTimePriorityPolicy;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AwexTest {

    private static final Integer SOME_VALUE = 42;
    private static final Integer SOME_OTHER_VALUE = 43;
    public static final String ANY_ERROR = "Argument not valid!";
    public static final String ANY_OTHER_ERROR = "Other not valid!";


    @Mock
    private ThreadHelper mThreadHelper;
    @Mock
    private Logger mLogger;

    private Awex mAwex;
    private Promise<Integer, Float> mTaskPromise;
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

        mTaskPromise = mAwex.submit(new Task<Integer, Float>() {
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

        Promise<Void, Void> promise = mAwex.submit(new VoidTask() {
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

        mTaskPromise = mAwex.submit(new Task<Integer, Float>() {
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

        Promise<Integer, Float> promise = mAwex.of(SOME_VALUE);

        assertTrue(promise.isResolved());
        assertEquals(SOME_VALUE, promise.getResult());
    }

    @Test
    public void shouldCreateARejectedPromiseWhenCreatedWithNullValue() {
        setUpAwex();

        Promise<Integer, Float> promise = mAwex.of(null);

        assertTrue(promise.isRejected());
    }

    @Test
    public void shouldCreateARejectedPromise() {
        setUpAwex();

        Promise<Integer, Float> promise = mAwex.absent();

        assertTrue(promise.isRejected());
    }

    @Test
    public void shouldInterruptTaskOnCancel() {
        setUpAwex();

        final Semaphore workIsRunning = new Semaphore(0);
        Promise<Integer, Float> promise = mAwex.submit(new Task<Integer, Float>() {

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
        mTaskPromise = mAwex.submit(new Task<Integer, Float>() {

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

        Promise<Void, Void> waitingPromise = mAwex.submit(someWaitingTask);
        Promise<Void, Void> realTimePromise = mAwex.submit(realTimeTask);

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

        Promise<Void, Void> waitingPromise = mAwex.submit(someWaitingTask);
        mAwex.submit(new VoidTask() {
            @Override
            protected void runWithoutResult() throws InterruptedException {

            }
        });
        Promise<Void, Void> realTimePromise = mAwex.submit(realTimeTask);

        realTimePromise.getResult();

        semaphore.release();
        waitingPromise.getResult();
    }

    @Test
    public void shouldCreateAllOfPromise() {
        setUpAwex();

        Promise<Collection<Integer>, Float> allOfPromise = mAwex.allOf(new AwexPromise<Integer, Float>(mAwex),
                new AwexPromise<Integer, Float>(mAwex));

        assertThat(allOfPromise, instanceOf(AllOfPromise.class));
    }

    @Test
    public void shouldCreateAnyOfPromise() {
        setUpAwex();

        Promise<Integer, Float> anyOfPromise = mAwex.anyOf(new AwexPromise<Integer, Float>(mAwex),
                new AwexPromise<Integer, Float>(mAwex));

        assertThat(anyOfPromise, instanceOf(AnyOfPromise.class));
    }

    @Test
    public void shouldCreateAfterAllPromise() {
        setUpAwex();

        Promise<MultipleResult<Integer, Float>, Float> afterAllPromise = mAwex.afterAll(
                new AwexPromise<Integer, Float>(mAwex),
                new AwexPromise<Integer, Float>(mAwex));

        assertThat(afterAllPromise, instanceOf(AfterAllPromise.class));
    }


    @Test
    public void shouldReturnFirstCorrectResultWhenSequentiallyUntilFirstDone() throws Exception {
        setUpAwex();
        Task<Integer, Float> incorrectTask1 = givenErrorTask(ANY_ERROR);
        Task<Integer, Float> incorrectTask2 = givenErrorTask(ANY_OTHER_ERROR);
        Task<Integer, Float> correctTask1 = givenCorrectTask(SOME_VALUE);
        Task<Integer, Float> correctTask2 = givenCorrectTask(SOME_OTHER_VALUE);
        List<Task<Integer, Float>> tasks = Arrays.<Task<Integer, Float>>asList(new Task[] {incorrectTask1,
                incorrectTask2,
                correctTask1,
                correctTask2});

        Promise<Integer, Float> mTasksPromise = mAwex.sequentiallyUntilFirstDone(tasks);

        assertEquals(SOME_VALUE, mTasksPromise.getResult());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void shouldReturnLastErrorWhenSequentiallyUntilFirstDoneAndAllTheTasksThrowErrors() throws Exception {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(ANY_OTHER_ERROR);

        setUpAwex();
        Task<Integer, Float> incorrectTask1 = givenErrorTask(ANY_ERROR);
        Task<Integer, Float> incorrectTask2 = givenErrorTask(ANY_ERROR);
        Task<Integer, Float> incorrectTask3 = givenErrorTask(ANY_OTHER_ERROR);
        List<Task<Integer, Float>> tasks = Arrays.<Task<Integer, Float>>asList(new Task[] {incorrectTask1,
                incorrectTask2, incorrectTask3});

        Promise<Integer, Float> mTasksPromise = mAwex.sequentiallyUntilFirstDone(tasks);

        mTasksPromise.getResult();
    }

    @Test(expected = EmptyTasksException.class)
    public void shouldThrowEmptyTasksExceptionWhenSequentiallyUntilFirstDoneAndNoTasks() throws Exception {
        setUpAwex();
        List<Task<Integer, Float>> tasks = new ArrayList<>();

        Promise<Integer, Float> mTasksPromise = mAwex.sequentiallyUntilFirstDone(tasks);

        mTasksPromise.getResult();
    }

    @Test
    public void shouldAbortTaskInQueueIfTimeoutExpires() {
        setUpAwex();

        final Semaphore semaphore = new Semaphore(0);

        mAwex.submit(new Task<Integer, Float>() {

            @Override
            protected Integer run() throws InterruptedException {
                semaphore.acquire();
                return null;
            }
        });

        mTaskPromise = mAwex.submit(new Task<Integer, Float>(Task.PRIORITY_NORMAL, 500, 500) {
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

        mTaskPromise = mAwex.submit(new Task<Integer, Float>(Task.PRIORITY_NORMAL, 500, 500) {
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

    private Task<Integer, Float> givenErrorTask(final String messageError) {
        return new Task<Integer, Float>() {
            @Override
            protected Integer run() throws InterruptedException {
                throw new IllegalArgumentException(messageError);
            }
        };
    }

    private Task<Integer, Float> givenCorrectTask(final int result) {
        return new Task<Integer, Float>() {
            @Override
            protected Integer run() throws InterruptedException {
                return result;
            }
        };
    }



    private void setUpAwex() {
        mAwex = new Awex(mThreadHelper, new ConsoleLogger(), new LinearWithRealTimePriorityPolicy(0, 1));
    }

}