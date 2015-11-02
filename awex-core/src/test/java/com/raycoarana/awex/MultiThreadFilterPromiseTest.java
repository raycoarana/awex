package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MultiThreadFilterPromiseTest extends BasePromiseTest {


    private AwexPromise<Collection<Integer>> mCollectionPromise;
    private AwexPromise<Integer> mPromise;
    private Promise<Collection<Integer>> mFilteredValue;

    @Test
    public void shouldFilterAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mCollectionPromise.filterParallel(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value > 2;
            }
        });

        mCollectionPromise.resolve(Arrays.asList(1, 2, 3, 4, 5));

        Collection<Integer> results = mFilteredValue.getResult();
        assertThat(results, hasItems(3, 4, 5));
    }

    @Test
    public void shouldFilterAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterParallel(new Filter<Integer>() {
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

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterParallel(new Filter<Integer>() {
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

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterParallel(new Filter<Integer>() {
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

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterParallel(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
