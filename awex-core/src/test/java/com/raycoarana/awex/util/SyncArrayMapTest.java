package com.raycoarana.awex.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SyncArrayMapTest {

    private SyncArrayMap<Object, Object> mSyncArrayMap;

    @Test
    public void shouldRemoveAnyExingingEntryWhenCrear() {
        mSyncArrayMap = new SyncArrayMap<>();
        mSyncArrayMap.put("ONE", "TWO");
        assertEquals(1, mSyncArrayMap.size());
        mSyncArrayMap.clear();
        assertEquals(0, mSyncArrayMap.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailToGetValuesIterable() {
        mSyncArrayMap = new SyncArrayMap<>();
        mSyncArrayMap.put("ONE", "TWO");
        mSyncArrayMap.values();
    }

}
