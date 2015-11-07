package com.raycoarana.awex;

import org.junit.Test;

public class TaskTest {

    private static final Integer SOME_VALUE = 42;

    @Test(expected = IllegalStateException.class)
    public void shouldFailToResetNotSubmittedTask() {
        Task<Integer> task = givenSomeTask();

        task.reset();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetPromiseNotSubmittedTask() {
        Task<Integer> task = givenSomeTask();

        task.getPromise();
    }

    private Task<Integer> givenSomeTask() {
        return new Task<Integer>() {
                @Override
                protected Integer run() throws InterruptedException {
                    return SOME_VALUE;
                }
            };
    }

}
