package com.raycoarana.awex;

class Worker implements Runnable {

    private final long mId;
    private final Thread mThread;
    private final AwexWorkQueue mWorkQueue;
    private final Logger mLogger;

    private boolean mDie = false;

    public Worker(long id, AwexWorkQueue workQueue, Logger logger) {
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
                    Work<?> work = mWorkQueue.take();
                    if (work != null) {
                        mLogger.v("Worker " + mId + " start executing work " + work.getId());
                        work.execute();
                        mLogger.v("Worker " + mId + " ends executing work " + work.getId());
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            }
        } finally {
            mLogger.v("Worker " + mId + " dies");
        }
    }

    public void die() {
        mDie = true;
    }

    public void interrupt() {
        mThread.interrupt();
    }

}