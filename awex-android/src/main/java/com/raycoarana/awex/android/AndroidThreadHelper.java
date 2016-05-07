package com.raycoarana.awex.android;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.raycoarana.awex.ThreadHelper;

/**
 * Android basic implementation of ThreadHelper
 */
public class AndroidThreadHelper implements ThreadHelper {

	private final Handler mHandler;

	public AndroidThreadHelper() {
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public boolean isCurrentThread() {
		if (VERSION.SDK_INT >= VERSION_CODES.M) {
			return Looper.getMainLooper().isCurrentThread();
		} else {
			return Thread.currentThread() == Looper.getMainLooper().getThread();
		}
	}

	@Override
	public void post(Runnable runnable) {
		mHandler.post(runnable);
	}

	@Override
	public void setUpPriorityToCurrentThread(int priority) {
        android.os.Process.setThreadPriority(priority);
	}

	@Override
	public void setUpPriorityToRealTimeThread() {
		setUpPriorityToCurrentThread(Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE);
	}

}