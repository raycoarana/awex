package com.raycoarana.awex;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AwexTest {

    private static final Integer SOME_VALUE = 42;

    @Mock
    private UIThread mUIThread;
    @Mock
    private Logger mLogger;

    private Awex mAwex;
    private Promise<Integer> mWorkPromise;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldExecuteSimpleWork() throws Exception {
        setUpAwex();

        mWorkPromise = mAwex.submit(new Task<Integer>() {
            @Override
            protected Integer run() throws InterruptedException {
                return SOME_VALUE;
            }
        });

        assertEquals(SOME_VALUE, mWorkPromise.getResult());
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

    private void setUpAwex() {
        mAwex = new Awex(mUIThread, mLogger, 1, 1);
    }

}