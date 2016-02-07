package com.raycoarana.awex.android;

import android.util.Log;

import com.raycoarana.awex.Logger;

/**
 * Android basic implementation for Logger
 */
public class AndroidLogger implements Logger {

    private static final String TAG = "AWEX";

    @Override
    public boolean isEnabled() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void v(String message) {
        Log.v(TAG, message);
    }

    @Override
    public void e(String message, Exception ex) {
        Log.e(TAG, message, ex);
    }

}