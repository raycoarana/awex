package com.raycoarana.awex;

/**
 * Base class for any work that will create some object as a result. Make sure you implement a safe way to cancel this
 * work when the state is changed to STATE_CANCELLING. Works will have some minor time to successfully exit before
 * a more aggressive cancel will be tried (interrupting the thread). You should not catch any InterruptedException
 * and let it abort the work, just do whatever you need to safely abort the work.
 *
 * @see VoidWork for works that doesn't returns any object
 */
public abstract class Work<T> {

	public static final int STATE_NOT_QUEUE = 0;
	public static final int STATE_QUEUE = 1;
	public static final int STATE_RUNNING = 2;
	public static final int STATE_FINISHED = 3;
	public static final int STATE_CANCELLING = 4;
	public static final int STATE_CANCELLED = 5;

	public static final int PRIORITY_LOWEST = 1;
	public static final int PRIORITY_LOW = 2;
	public static final int PRIORITY_NORMAL = 3;
	public static final int PRIORITY_HIGH = 4;
	public static final int PRIORITY_REAL_TIME = Integer.MAX_VALUE;

	private final Logger mLogger;
	private final long mId;
	private final AwexPromise<T> mPromise;
	private final int mPriority;

	private int mCurrentState = STATE_NOT_QUEUE;

	public Work(Awex awex) {
		this(awex, PRIORITY_NORMAL);
	}

	public Work(Awex awex, int priority) {
		mId = awex.provideWorkId();
		mPriority = priority;
		mPromise = new AwexPromise<>(awex, this);
		mLogger = awex.provideLogger();
		printStateChanged("NOT_QUEUE");
	}

	public long getId() {
		return mId;
	}

	public int getPriority() {
		return mPriority;
	}

	public int getState() {
		return mCurrentState;
	}

	public Promise<T> getPromise() {
		return mPromise;
	}

	protected abstract T run() throws InterruptedException;

	final void execute() {
		mCurrentState = STATE_RUNNING;
		printStateChanged("RUNNING");
		try {
			T result = run();
			if (mCurrentState == STATE_CANCELLING) {
				mCurrentState = STATE_CANCELLED;
				printStateChanged("CANCELLED");
				return;
			}
			mPromise.resolve(result);
		} catch (Exception ex) {
			mPromise.reject(ex);
		}
		mCurrentState = STATE_FINISHED;
		printStateChanged("FINISHED");
	}

	final void softCancel() {
		mCurrentState = STATE_CANCELLING;
		printStateChanged("CANCELLING");
	}

	final void markQueue() {
		mCurrentState = STATE_QUEUE;
		printStateChanged("QUEUE");
	}

	private void printStateChanged(String newState) {
		mLogger.v("Work " + mId + " state changed to " + newState);
	}
}