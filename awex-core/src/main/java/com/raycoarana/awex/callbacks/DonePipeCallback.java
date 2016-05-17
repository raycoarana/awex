package com.raycoarana.awex.callbacks;

import com.raycoarana.awex.Promise;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 */
public interface DonePipeCallback<Result, Result_Out, Progress_Out> {

	DonePipeCallback EMPTY = null;

	Promise<Result_Out, Progress_Out> onDone(Result result);

}
