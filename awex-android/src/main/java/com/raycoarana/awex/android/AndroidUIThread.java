package com.raycoarana.awex.android;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;

import com.raycoarana.awex.UIThread;

/**
 * Android basic implementation of UIThread
 */
public class AndroidUIThread implements UIThread {

	private final Handler mHandler;

	public AndroidUIThread() {
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

}