package com.raycoarana.awex.sample;

import com.raycoarana.awex.Awex;
import com.raycoarana.awex.android.AndroidLogger;
import com.raycoarana.awex.android.AndroidUIThread;
import com.raycoarana.awex.policy.LinearWithRealTimePriority;

public class AwexProvider {

    private static final int MAX_THREADS = 4;

    private static Awex sInstance = null;

    public static synchronized Awex get() {
        if(sInstance == null) {
            sInstance = new Awex(new AndroidUIThread(), new AndroidLogger(), new LinearWithRealTimePriority(MAX_THREADS));
        }
        return sInstance;
    }

}
