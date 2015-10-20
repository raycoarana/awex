package com.raycoarana.awex;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Awex {

	private final UIThread mUIThread;
	private final Logger mLogger;
	private final ExecutorService mExecutor;
	private final AtomicLong mIdProvider = new AtomicLong();

	public Awex(UIThread uiThread, Logger logger) {
		mUIThread = uiThread;
		mLogger = logger;
		mExecutor = Executors.newFixedThreadPool(5);
	}

	UIThread provideUIThread() {
		return mUIThread;
	}

	Logger provideLogger() {
		return mLogger;
	}

	long provideWorkId() {
		return mIdProvider.incrementAndGet();
	}

	public <T> Promise<T> submit(final Work<T> work) {
		work.markQueue();
		mExecutor.submit(new Runnable() {
			@Override
			public void run() {
				work.execute();
			}
		});
		return work.getPromise();
	}

	void submit(Runnable runnable) {
		mExecutor.submit(runnable);
	}

	public <T> void cancel(Work<T> work) {
		work.softCancel();
		//TODO: check in some time if softCancel do the cancel and if not do hardCancel calling interrupt on the thread
		// that will sacrifice the thread and we will need to recreate a new one...
	}
}