package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingleThreadFilterPromiseTest extends BasePromiseTest {


    private AwexPromise<Collection<Integer>> mCollectionPromise;
    private AwexPromise<Integer> mPromise;
    private Promise<Collection<Integer>> mFilteredValue;

    @Test
    public void shouldFilterAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        mCollectionPromise = new AwexPromise<>(mAwex, mWork);

        mFilteredValue = mCollectionPromise.filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value > 2;
            }
        });

        mCollectionPromise.resolve(Arrays.asList(1, 2, 3, 4, 5));

        Integer[] results = mFilteredValue.getResult().toArray(new Integer[]{});
        assertEquals(3, (int) results[0]);
        assertEquals(4, (int) results[1]);
        assertEquals(5, (int) results[2]);
    }

    @Test
    public void shouldFilterAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);

        mFilteredValue = mPromise.filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value > 2;
            }
        });

        mPromise.resolve(1);

        assertTrue(mFilteredValue.getResult().isEmpty());
    }

    @Test
    public void shouldNotFilterAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);

        mFilteredValue = mPromise.filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value == 1;
            }
        });

        mPromise.resolve(1);

        assertEquals(1, (int) mFilteredValue.getResult().iterator().next());
    }

    @Test
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);

        mFilteredValue = mPromise.filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.reject(new Exception());

        assertEquals(Promise.STATE_REJECTED, mFilteredValue.getState());
    }

    @Test
    public void shouldCancelFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);

        mFilteredValue = mPromise.filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.cancelWork();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
