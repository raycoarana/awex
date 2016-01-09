package com.raycoarana.awex;

import com.raycoarana.awex.exceptions.AbsentValueException;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AwexCollectionPromiseTest extends BasePromiseTest {

    private Promise<Collection<Integer>> mPromise;

    @Test
    public void shouldGetFirstItemOfCollection() throws Exception {
        setUpAwex();

        mPromise = new AwexCollectionPromise<Integer>(mAwex).resolve(Arrays.asList(1, 2));

        assertEquals(1, (int) mPromise.<Integer>stream().singleOrFirst().getResult());
    }

    @Test
    public void shouldGetTheOnlyExistingItem() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<Integer>(mAwex).resolve(1).stream();

        assertEquals(1, (int) mPromise.<Integer>stream().singleOrFirst().getResult());
    }

    @Test(expected = AbsentValueException.class)
    public void shouldFailToGetResultOfCancelledPromise() throws Exception {
        setUpAwex();

        mPromise = new AwexCollectionPromise<Integer>(mAwex).resolve(Collections.<Integer>emptyList());

        mPromise.<Integer>stream().singleOrFirst().getResult();
    }

}