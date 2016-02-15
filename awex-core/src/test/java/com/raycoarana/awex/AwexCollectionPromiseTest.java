package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.exceptions.AbsentValueException;
import com.raycoarana.awex.transform.Func;
import com.raycoarana.awex.transform.Mapper;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AwexCollectionPromiseTest extends BasePromiseTest {

    @Mock
    private DoneCallback<Integer> mStep0DoneCallback;

    @Mock
    private DoneCallback<Integer> mStep1DoneCallback;

    @Mock
    private DoneCallback<Collection<Integer>> mDoneCallback;

    private Promise<Collection<Integer>, Float> mPromise;

    @Test
    public void shouldGetFirstItemOfCollection() throws Exception {
        setUpAwex();

        mPromise = new AwexCollectionPromise<Integer, Float>(mAwex).resolve(Arrays.asList(1, 2));

        assertEquals(1, (int) mPromise.<Integer>stream().singleOrFirst().getResult());
    }

    @Test
    public void shouldGetTheOnlyExistingItem() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<Integer, Float>(mAwex).resolve(1).stream();

        assertEquals(1, (int) mPromise.<Integer>stream().singleOrFirst().getResult());
    }

    @Test(expected = AbsentValueException.class)
    public void shouldFailToGetResultOfCancelledPromise() throws Exception {
        setUpAwex();

        mPromise = new AwexCollectionPromise<Integer, Float>(mAwex).resolve(Collections.<Integer>emptyList());

        mPromise.<Integer>stream().singleOrFirst().getResult();
    }

    @Test
    public void shouldDefferCalculationUntilAnyoneCallsGetResult() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Void> promise = new AwexPromise<>(mAwex, mTask);
        promise.resolve(Collections.singletonList(42));

        CollectionPromise<Integer, Void> step0 = promise.<Integer>stream().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep0DoneCallback.onDone(item);
            }
        });

        CollectionPromise<Integer, Void> step1 = step0.forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep1DoneCallback.onDone(item);
            }
        });

        step1.getResult();

        verify(mStep0DoneCallback).onDone(42);
        verify(mStep1DoneCallback).onDone(42);
    }

    @Test
    public void shouldDefferCalculationUntilAnyoneCallsGetResultOrDefault() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Void> promise = new AwexPromise<>(mAwex, mTask);
        promise.resolve(Collections.singletonList(42));

        CollectionPromise<Integer, Void> step0 = promise.<Integer>stream().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep0DoneCallback.onDone(item);
            }
        });

        CollectionPromise<Integer, Void> step1 = step0.forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep1DoneCallback.onDone(item);
            }
        });

        step1.getResultOrDefault(Collections.<Integer>emptyList());

        verify(mStep0DoneCallback).onDone(42);
        verify(mStep1DoneCallback).onDone(42);
    }

    @Test
    public void shouldDefferCalculationUntilAnyoneCallsSingleOrFirst() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Void> promise = new AwexPromise<>(mAwex, mTask);
        promise.resolve(Collections.singletonList(42));

        CollectionPromise<Integer, Void> step0 = promise.<Integer>stream().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep0DoneCallback.onDone(item);
            }
        });

        CollectionPromise<Integer, Void> step1 = step0.forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep1DoneCallback.onDone(item);
            }
        });

        step1.singleOrFirst();

        verify(mStep0DoneCallback).onDone(42);
        verify(mStep1DoneCallback).onDone(42);
    }

    @Test
    public void shouldDefferCalculationUntilAnyoneWantsTheResult() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Void> promise = new AwexPromise<>(mAwex, mTask);
        promise.resolve(Collections.singletonList(42));

        CollectionPromise<Integer, Void> step0 = promise.<Integer>stream().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep0DoneCallback.onDone(item);
            }
        });

        CollectionPromise<Integer, Void> step1 = step0.forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep1DoneCallback.onDone(item);
            }
        });

        step1.done(mDoneCallback);

        verify(mStep0DoneCallback).onDone(42);
        verify(mStep1DoneCallback).onDone(42);
    }

    @Test
    public void shouldDefferCalculationUntilAnyoneCallsApplyNow() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Void> promise = new AwexPromise<>(mAwex, mTask);
        promise.resolve(Collections.singletonList(42));

        CollectionPromise<Integer, Void> step0 = promise.<Integer>stream().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep0DoneCallback.onDone(item);
            }
        });

        CollectionPromise<Integer, Void> step1 = step0.applyNow().forEach(new Func<Integer>() {
            @Override
            public void run(Integer item) {
                mStep1DoneCallback.onDone(item);
            }
        });

        step0.getResult();
        step1.getResult();

        verify(mStep0DoneCallback).onDone(42);
        verify(mStep1DoneCallback).onDone(42);
    }

}