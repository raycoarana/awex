package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class SingleThreadMapperPromiseTest extends BasePromiseTest {

    private AwexPromise<Collection<Integer>> mCollectionPromise;
    private AwexPromise<Integer> mPromise;
    private Promise<Collection<String>> mFilteredValue;

    @Test
    public void shouldMapAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mCollectionPromise.map(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mCollectionPromise.resolve(Arrays.asList(1, 2, 3));

        String[] results = mFilteredValue.getResult().toArray(new String[]{});
        assertEquals("1", results[0]);
        assertEquals("2", results[1]);
        assertEquals("3", results[2]);
    }

    @Test
    public void shouldMapAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.map(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.resolve(1);

        assertEquals("1", mFilteredValue.getResult().iterator().next());
    }

    @Test
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.map(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.reject(new Exception());

        assertEquals(Promise.STATE_REJECTED, mFilteredValue.getState());
    }

    @Test
    public void shouldCancelFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mFilteredValue = mPromise.map(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mFilteredValue.getState());
    }

}
