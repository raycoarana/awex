package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class SingleThreadFilterPromiseTest extends BasePromiseTest {

    private AwexPromise<Collection<Integer>, Float> mPromise;
    private CollectionPromise<Integer, Float> mFilteredValue;

    @Test
    public void shouldFilterAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>, Float> mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mCollectionPromise.<Integer>stream().filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value > 2;
            }
        });

        mCollectionPromise.resolve(Arrays.asList(1, 2, 3, 4, 5));

        Collection<Integer> result = mFilteredValue.getResult();
        Integer[] results = result.toArray(new Integer[result.size()]);
        assertEquals(3, (int) results[0]);
        assertEquals(4, (int) results[1]);
        assertEquals(5, (int) results[2]);
    }

    @Test
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.<Integer>stream().filter(new Filter<Integer>() {
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

        mFilteredValue = mPromise.<Integer>stream().filter(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
