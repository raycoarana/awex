package com.raycoarana.awex;

class Worker implements Runnable {

    private final long mId;
    private final Thread mThread;
    private final AwexTaskQueue mWorkQueue;
    private final Logger mLogger;

    private boolean mDie = false;

    public Worker(long id, AwexTaskQueue workQueue, Logger logger) {
        mId = id;
        mThread = new Thread(this, "Awex worker " + id);
        mWorkQueue = workQueue;
        mLogger = logger;

        mThread.start();
    }

    @Override
    public void run() {
        mLogger.v("Worker " + mId + " starting...");
        try {
            while (!mDie) {
                try {
                    Task<?> task = mWorkQueue.take(this);
                    if (task != null) {
                        mLogger.v("Worker " + mId + " start executing task " + task.getId());
                        task.execute();
                        mLogger.v("Worker " + mId + " ends executing task " + task.getId());
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            }
        } finally {
            mLogger.v("Worker " + mId + " dies");
        }
    }

    public void interrupt() {
        mDie = true;
        mThread.interrupt();
    }

}