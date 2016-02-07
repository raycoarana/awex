package com.raycoarana.awex.state;

public interface QueueState {
    int getId();

    int getEnqueue();

    int getWaiters();

    int numberOfWorkers();
}
