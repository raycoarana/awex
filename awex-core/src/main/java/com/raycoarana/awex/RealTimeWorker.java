package com.raycoarana.awex;

class RealTimeWorker implements Runnable {

    private final long mId;
    private final Task mTask;
    private final Logger mLogger;

    public RealTimeWorker(long id, Task task, Logger logger) {
        mId = id;
        Thread thread = new Thread(this, "Awex real-time worker " + id);
        mTask = task;
        mLogger = logger;

        thread.start();
    }

    @Override
    public void run() {
        if (mLogger.isEnabled()) {
            mLogger.v("Worker " + mId + " starting...");
        }
        try {
            if (mLogger.isEnabled()) {
                mLogger.v("Worker " + mId + " start executing task " + mTask.getId());
            }
            mTask.execute();
            if (mLogger.isEnabled()) {
                mLogger.v("Worker " + mId + " ends executing task " + mTask.getId());
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (mLogger.isEnabled()) {
                mLogger.v("Worker " + mId + " dies");
            }
        }
    }

}
