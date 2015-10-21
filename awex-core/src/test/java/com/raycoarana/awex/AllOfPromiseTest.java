package com.raycoarana.awex;

import com.raycoarana.awex.exceptions.AllFailException;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AllOfPromiseTest extends BasePromiseTest {

    private static final Integer SOME_RESULT_VALUE = 42;
    private static final Integer SOME_OTHER_RESULT_VALUE = 45;

    private AwexPromise<Integer> mFirstPromise;
    private AwexPromise<Integer> mSecondPromise;
    private AllOfPromise<Integer> mAllOfPromise;

    @Test
    public void shouldReturnValueOfFirstPromise() throws Exception {
        setUpAwex();

        setUpPromises();

        mFirstPromise.resolve(SOME_RESULT_VALUE);
        mSecondPromise.resolve(SOME_OTHER_RESULT_VALUE);

        assertThat(mAllOfPromise.getResult(), hasItems(SOME_RESULT_VALUE, SOME_OTHER_RESULT_VALUE));
    }

    @Test(expected = Exception.class)
    public void shouldFailIfAnyPromisesFails() throws Exception {
        setUpAwex();

        setUpPromises();

        mSecondPromise.reject(new Exception("Second"));

        mAllOfPromise.getResult();
    }

    @Test(expected = Exception.class)
    public void shouldFailIfAnyPromisesFailsEvenIfOneIsResolved() throws Exception {
        setUpAwex();

        setUpPromises();

        mFirstPromise.resolve(SOME_RESULT_VALUE);
        mSecondPromise.reject(new Exception("Second"));

        mAllOfPromise.getResult();
    }

    @Test
    public void shouldBeCancelledOnceAnyPromiseIsCancelled() throws Exception {
        setUpAwex();

        setUpPromises();

        mSecondPromise.cancelWork();

        assertTrue(mAllOfPromise.isCancelled());
    }

    private void setUpPromises() {
        mFirstPromise = new AwexPromise<>(mAwex, mWork);
        mSecondPromise = new AwexPromise<>(mAwex, mWork);
        mAllOfPromise = new AllOfPromise<>(mAwex,
                Arrays.<Promise<Integer>>asList(mFirstPromise, mSecondPromise));
    }

}
