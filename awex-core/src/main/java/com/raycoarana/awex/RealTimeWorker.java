package com.raycoarana.awex;

class RealTimeWorker implements Runnable {

    private final long mId;
    private final Thread mThread;
    private final Task mTask;
    private final Logger mLogger;

    private boolean mDie = false;

    public RealTimeWorker(long id, Task task, Logger logger) {
        mId = id;
        mThread = new Thread(this, "Awex real-time worker " + id);
        mTask = task;
        mLogger = logger;

        mThread.start();
    }

    @Override
    public void run() {
        mLogger.v("Worker " + mId + " starting...");
        try {
            try {
                mLogger.v("Worker " + mId + " start executing task " + mTask.getId());
                mTask.execute();
                mLogger.v("Worker " + mId + " ends executing task " + mTask.getId());
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } finally {
            mLogger.v("Worker " + mId + " dies");
        }
    }
}
