package com.raycoarana.awex;

import com.raycoarana.awex.transform.Mapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapperTransformerPromiseTest extends BasePromiseTest {

    private AwexPromise<Integer> mPromise;
    private Promise<String> mMappedValue;

    @Test
    public void shouldMapAResolvedPromiseWithSingleValue() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mPromise.mapSingle(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.resolve(1);

        assertEquals("1", mMappedValue.getResult());
    }

    @Test
    public void shouldRejectMappedPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mPromise.mapSingle(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.reject(new Exception());

        assertEquals(Promise.STATE_REJECTED, mMappedValue.getState());
    }

    @Test
    public void shouldCancelMappedPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        mMappedValue = mPromise.mapSingle(new Mapper<Integer, String>() {
            @Override
            public String map(Integer value) {
                return String.valueOf(value);
            }
        });

        mPromise.cancelTask();

        assertEquals(Promise.STATE_CANCELLED, mMappedValue.getState());
    }

}
