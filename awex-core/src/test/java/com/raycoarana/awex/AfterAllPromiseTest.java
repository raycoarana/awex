package com.raycoarana.awex;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AfterAllPromiseTest extends BasePromiseTest {

    private static final Integer SOME_RESULT_VALUE = 42;
    private static final Integer SOME_OTHER_RESULT_VALUE = 45;
    private static final Integer SOME_ALTERNATIVE_VALUE = 66;

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final Exception SOME_OTHER_EXCEPTION_VALUE = new Exception();

    private AwexPromise<Integer> mFirstPromise;
    private AwexPromise<Integer> mSecondPromise;
    private AfterAllPromise<Integer> mAfterAllPromise;
    private MultipleResult<Integer> mResult;

    @Test
    public void shouldReturnAllResultsWhenBothResolved() throws Exception {
        setUpAwex();

        setUpPromises();

        mFirstPromise.resolve(SOME_RESULT_VALUE);
        mSecondPromise.resolve(SOME_OTHER_RESULT_VALUE);

        mResult = mAfterAllPromise.getResult();

        assertEquals(Promise.STATE_RESOLVED, mResult.getState(0));
        assertEquals(Promise.STATE_RESOLVED, mResult.getState(1));
        assertEquals(SOME_RESULT_VALUE, mResult.getResult(0));
        assertEquals(SOME_OTHER_RESULT_VALUE, mResult.getResult(1));
    }

    @Test
    public void shouldReturnOneResultAndOneExceptionWhenFirstIsResolvedAndSecondIsFailed() throws Exception {
        setUpAwex();

        setUpPromises();

        mFirstPromise.resolve(SOME_RESULT_VALUE);
        mSecondPromise.reject(SOME_OTHER_EXCEPTION_VALUE);

        mResult = mAfterAllPromise.getResult();

        assertEquals(Promise.STATE_RESOLVED, mResult.getState(0));
        assertEquals(Promise.STATE_REJECTED, mResult.getState(1));
        assertEquals(SOME_RESULT_VALUE, mResult.getResult(0));
        assertEquals(SOME_ALTERNATIVE_VALUE, mResult.getResultOrDefault(1, SOME_ALTERNATIVE_VALUE));
    }

    @Test
    public void shouldBeCancelledOnceAnyPromiseIsCancelled() throws Exception {
        setUpAwex();

        setUpPromises();

        mSecondPromise.cancelTask();

        assertTrue(mAfterAllPromise.isCancelled());
    }

    private void setUpPromises() {
        mFirstPromise = new AwexPromise<>(mAwex, mTask);
        mSecondPromise = new AwexPromise<>(mAwex, mTask);
        mAfterAllPromise = new AfterAllPromise<>(mAwex,
                Arrays.<Promise<Integer>>asList(mFirstPromise, mSecondPromise));
    }

}
