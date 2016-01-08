package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class MultiThreadMapperPromiseTest extends BasePromiseTest {

    private AwexPromise<Collection<Integer>> mPromise;
    private CollectionPromise<String> mMappedValue;

    @Test
    public void shouldMapAResolvedPromiseWithCollection() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Integer>> mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mCollectionPromise.<Integer>stream().mapParallel(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mCollectionPromise.resolve(Arrays.asList(1, 2, 3));

        Collection<String> result = mMappedValue.getResult();
        String[] results = result.toArray(new String[result.size()]);
        assertEquals("1", results[0]);
        assertEquals("2", results[1]);
        assertEquals("3", results[2]);
    }

    @Test
    public void shouldRejectFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mPromise.<Integer>stream().mapParallel(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.reject(new Exception());

        assertEquals(Promise.STATE_REJECTED, mMappedValue.getState());
    }

    @Test
    public void shouldCancelFilteredPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mPromise.<Integer>stream().mapParallel(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mMappedValue.getState());
    }

}
