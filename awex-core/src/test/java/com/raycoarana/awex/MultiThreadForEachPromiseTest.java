package com.raycoarana.awex;

import com.raycoarana.awex.model.Item;
import com.raycoarana.awex.transform.Func;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class MultiThreadForEachPromiseTest extends BasePromiseTest {

    @Test
    public void shouldIterateOverAllItemsOfAResolvedPromise() throws Exception {
        setUpAwex();

        AwexPromise<Collection<Item>> mCollectionPromise = new AwexPromise<>(mAwex, mTask);

        CollectionPromise<Item> mResultPromise = mCollectionPromise.<Item>stream().forEachParallel(new Func<Item>() {
            @Override
            public void run(Item item) {
                item.setValue(item.getValue() + 1);
            }
        });

        mCollectionPromise.resolve(Arrays.asList(new Item(1),
                new Item(2),
                new Item(3)));

        Collection<Item> result = mResultPromise.getResult();
        Item[] results = result.toArray(new Item[result.size()]);
        assertEquals(2, results[0].getValue());
        assertEquals(3, results[1].getValue());
        assertEquals(4, results[2].getValue());
    }

}
