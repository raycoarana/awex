package com.raycoarana.awex.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArrayMapTest {

    private ArrayMap<Object, Object> mArrayMap;

    @Test
    public void shouldRemoveAnyExingingEntryWhenCrear() {
        mArrayMap = new ArrayMap<>(0);
        mArrayMap.put("ONE", "TWO");
        assertEquals(1, mArrayMap.size());
        mArrayMap.clear();
        assertEquals(0, mArrayMap.size());
    }

    @Test
    public void shouldContainsValue() {
        mArrayMap = new ArrayMap<>(0);
        mArrayMap.put("ONE", "TWO");
        assertTrue(mArrayMap.containsValue("TWO"));
    }

    @Test
    public void shouldOverrideExistinValue() {
        mArrayMap = new ArrayMap<>(2);
        mArrayMap.put("ONE", "TWO");
        mArrayMap.put("ONE", "ONE");
        assertFalse(mArrayMap.containsValue("TWO"));
        assertTrue(mArrayMap.containsValue("ONE"));
    }

    @Test
    public void shouldPutAll() {
        mArrayMap = new ArrayMap<>(1);
        mArrayMap.put("ONE", "TWO");
        ArrayMap<Object, Object> second = new ArrayMap<>();
        second.put("OTHER", "VALUE");
        second.putAll(mArrayMap);
        assertEquals(2, second.size());
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    public void shouldBeEqualsIfContainsTheSameData() {
        mArrayMap = new ArrayMap<>(1);
        mArrayMap.put("ONE", "TWO");
        ArrayMap<Object, Object> second = new ArrayMap<>();
        second.put("ONE", "TWO");
        assertTrue(second.equals(mArrayMap));
        assertTrue(mArrayMap.equals(mArrayMap));
        second.clear();
        assertFalse(mArrayMap.equals(second));
        second.put("OTHER", "VALUE");
        assertFalse(mArrayMap.equals(second));
        second.clear();
        second.put("ONE", "VALUE");
        assertFalse(mArrayMap.equals(second));
        assertFalse(mArrayMap.equals(new Object()));
    }

    @Test
    public void shouldPrintDataIfEmpty() {
        mArrayMap = new ArrayMap<>(1);
        assertEquals("{}", mArrayMap.toString());
    }

    @Test
    public void shouldPrintDataIfHasValue() {
        mArrayMap = new ArrayMap<>(1);
        mArrayMap.put("ONE", "TWO");
        mArrayMap.put("TWO", "THREE");
        assertEquals("{ONE=TWO, TWO=THREE}", mArrayMap.toString());
    }

    @Test
    public void shouldNotFailWithStackoverflow() {
        mArrayMap = new ArrayMap<>(1);
        mArrayMap.put("ONE", mArrayMap);
        mArrayMap.put(mArrayMap, "THREE");
        assertEquals("{ONE=(this Map), (this Map)=THREE}", mArrayMap.toString());
    }

}
