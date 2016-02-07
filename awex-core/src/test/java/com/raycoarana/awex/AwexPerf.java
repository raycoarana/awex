package com.raycoarana.awex;

import com.raycoarana.awex.policy.LinearWithRealTimePriority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AwexPerf extends BasePerf {

    int numberOfTasks = 1000;

    private UIThread mUIThread = new UIThread() {
        @Override
        public boolean isCurrentThread() {
            return false;
        }

        @Override
        public void post(Runnable runnable) {

        }
    };
    private Logger mLogger = new Logger() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void v(String message) {

        }

        @Override
        public void e(String message, Exception ex) {

        }
    };

    private Awex mAwex;
    private ExecutorService mThreadPool;

    @Before
    public void setUp() {
        super.setUp();
        mAwex = new Awex(mUIThread, mLogger, new LinearWithRealTimePriority(1));
        mThreadPool = Executors.newFixedThreadPool(1);
    }

    @Test
    public void benchSubmitTask() throws Exception {
        List<Promise<Integer, Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(mAwex.submit(new Task<Integer, Void>() {
                @Override
                protected Integer run() throws InterruptedException {
                    return 1;
                }
            }));
        }
        mAwex.afterAll(tasks).getResult();
    }

    @Test
    public void benchReference() throws Exception {
        List<Future<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(mThreadPool.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return 1;
                }
            }));
        }
        for(Future<Integer> future : tasks) {
            future.get();
        }
    }

}
