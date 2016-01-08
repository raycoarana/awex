package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MultiThreadFilterPromiseTest extends BasePromiseTest {


    private AwexPromise<Integer> mPromise;
    private CollectionPromise<Integer> mFilteredValue;

    @Test
    public void shouldFilterAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>> mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mCollectionPromise.<Integer>stream().filterParallel(new Filter<Integer>() {
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
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.<Integer>stream().filterParallel(new Filter<Integer>() {
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

        mFilteredValue = mPromise.<Integer>stream().filterParallel(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
