package com.raycoarana.awex;

import com.raycoarana.awex.transform.Filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilterTransformerPromiseTest extends BasePromiseTest {

    private AwexPromise<Integer, Float> mPromise;
    private Promise<Integer, Float> mFilteredValue;

    @Test
    public void shouldFilterAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterSingle(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value > 2;
            }
        });

        mPromise.resolve(1);

        assertEquals(Promise.STATE_REJECTED, mFilteredValue.getState());
    }

    @Test
    public void shouldNotFilterAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterSingle(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return value == 1;
            }
        });

        mPromise.resolve(1);

        assertEquals(1, (int) mFilteredValue.getResult());
    }

    @Test
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.filterSingle(new Filter<Integer>() {
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

        mFilteredValue = mPromise.filterSingle(new Filter<Integer>() {
            @Override
            public boolean filter(Integer value) {
                return false;
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
