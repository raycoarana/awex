package com.raycoarana.awex;

import com.raycoarana.awex.exceptions.OrException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrPromiseTest extends BasePromiseTest {

    private static final Integer SOME_RESULT_VALUE = 42;

    private AwexPromise<Integer, Float> mMainPromise;
    private AwexPromise<Integer, Float> mSecondChoicePromise;
    private OrPromise<Integer, Float> mOrPromise;

    @Test
    public void shouldReturnValueOfMainPromise() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.resolve(SOME_RESULT_VALUE);

        assertEquals(mOrPromise.getResult(), SOME_RESULT_VALUE);
    }

    @Test
    public void shouldReturnValueOfSecondChoiceWhenMainPromiseFails() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.reject(new Exception());
        mSecondChoicePromise.resolve(SOME_RESULT_VALUE);

        assertEquals(mOrPromise.getResult(), SOME_RESULT_VALUE);
    }

    @Test(expected = OrException.class)
    public void shouldFailIfBothMainAndSecondChoicePromisesFails() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.reject(new Exception("Main"));
        mSecondChoicePromise.reject(new Exception("SecondChoice"));

        mOrPromise.getResult();
    }

    @Test
    public void shouldCreateOrExceptionWithMainAndSecondChoicePromiseExceptions() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.reject(new Exception("Main"));
        mSecondChoicePromise.reject(new Exception("SecondChoice"));

        try {
            mOrPromise.getResult();
            throw new IllegalStateException("Invalid promise state");
        } catch (OrException ex) {
            assertEquals("Main", ex.getMainPromiseException().getMessage());
            assertEquals("SecondChoice", ex.getSecondChoicePromiseException().getMessage());
        }
    }

    @Test
    public void shouldBeCancelledOnceMainPromiseIsCancelled() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.cancelTask();

        assertTrue(mOrPromise.isCancelled());
    }

    @Test
    public void shouldBeCancelledOnceMainIsRejectedAndSecondChoicePromiseIsCancelled() throws Exception {
        setUpAwex();

        setUpPromises();

        mMainPromise.reject(new Exception());
        mSecondChoicePromise.cancelTask();

        assertTrue(mOrPromise.isCancelled());
    }

    private void setUpPromises() {
        mMainPromise = new AwexPromise<>(mAwex, mTask);
        mSecondChoicePromise = new AwexPromise<>(mAwex, mTask);
        mOrPromise = new OrPromise<>(mAwex, mMainPromise, mSecondChoicePromise);
    }

}
