package com.raycoarana.awex;

public interface ThreadHelper {

    boolean isCurrentThread();

    void post(Runnable runnable);

    void setUpPriorityToCurrentThread(int priority);

    void setUpPriorityToRealTimeThread();
}