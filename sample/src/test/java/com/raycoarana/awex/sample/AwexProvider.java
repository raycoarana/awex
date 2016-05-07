package com.raycoarana.awex.sample;

import com.raycoarana.awex.Awex;
import com.raycoarana.awex.android.AndroidLogger;
import com.raycoarana.awex.android.AndroidThreadHelper;
import com.raycoarana.awex.policy.LinearWithRealTimePriorityPolicy;

public class AwexProvider {

    private static final int MAX_THREADS = 4;

    private static Awex sInstance = null;

    public static synchronized Awex get() {
        if(sInstance == null) {
            sInstance = new Awex(new AndroidThreadHelper(), new AndroidLogger(), new LinearWithRealTimePriorityPolicy(MAX_THREADS));
        }
        return sInstance;
    }

}
