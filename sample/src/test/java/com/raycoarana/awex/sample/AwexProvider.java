package com.raycoarana.awex.sample;

import com.raycoarana.awex.Awex;
import com.raycoarana.awex.android.AndroidLogger;
import com.raycoarana.awex.android.AndroidUIThread;

public class AwexProvider {

    private static final int MIN_THREADS = 2;
    private static final int MAX_THREADS = 4;

    private static Awex sInstance = null;

    public static synchronized Awex get() {
        if(sInstance == null) {
            sInstance = new Awex(new AndroidUIThread(), new AndroidLogger(), MIN_THREADS, MAX_THREADS);
        }
        return sInstance;
    }

}
