package com.raycoarana.awex;

public interface PoolManager {

    void createQueue(int queueId);

    void executeImmediately(Task task);
    void queueTask(int queueId, Task task);
    void mergeTask(Task taskInQueue, Task taskToMerge);

    long createWorker(int queueId);
    void removeWorker(int queueId, long workerId);
    void removeWorker(int queueId, long workerId, boolean shouldInterrupt);

}