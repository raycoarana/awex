package com.raycoarana.awex.callbacks;

import com.raycoarana.awex.Promise;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 */
public interface FailPipeCallback<Result_Out, Progress_Out> {

	FailPipeCallback EMPTY = null;

	Promise<Result_Out, Progress_Out> onFail(Exception exception);

}
