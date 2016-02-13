package com.raycoarana.awex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TaskTest {

    private static final Integer SOME_VALUE = 42;

    @Test(expected = IllegalStateException.class)
    public void shouldFailToResetNotSubmittedTask() {
        Task<Integer, Float> task = givenSomeTask();

        task.reset();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetPromiseNotSubmittedTask() {
        Task<Integer, Float> task = givenSomeTask();

        task.getPromise();
    }

    @Test
    public void shouldPrintCurrentStateOfTask() {
        Task<Integer, Float> task = givenSomeTask();

        assertEquals("{/*com.raycoarana.awex.TaskTest$1*/ id: 0, state: -1, priority: 3 }", task.toString());
    }

    private Task<Integer, Float> givenSomeTask() {
        return new Task<Integer, Float>() {
                @Override
                protected Integer run() throws InterruptedException {
                    return SOME_VALUE;
                }
            };
    }

}
