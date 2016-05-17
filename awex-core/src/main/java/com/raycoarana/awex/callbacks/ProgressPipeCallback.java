package com.raycoarana.awex.callbacks;

import com.raycoarana.awex.Promise;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 */
public interface ProgressPipeCallback<Progress, Result_Out, Progress_Out> {

	ProgressPipeCallback EMPTY = null;

	Promise<Result_Out, Progress_Out> onProgress(Progress progress);

}
