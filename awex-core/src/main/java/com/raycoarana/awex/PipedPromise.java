package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.DonePipeCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.FailPipeCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.callbacks.ProgressPipeCallback;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 */
public class PipedPromise<Result, Progress, Result_Out, Progress_Out> extends AwexPromise<Result_Out, Progress_Out> {

	protected PipedPromise(final AwexPromise<Result, Progress> awexPromise, final DonePipeCallback<Result, Result_Out,
			Progress_Out> donePipeCallback, final FailPipeCallback<Result_Out, Progress_Out> failPipeCallback,
			final ProgressPipeCallback<Progress, Result_Out, Progress_Out> progressPipeCallback) {

		super(awexPromise.mAwex, awexPromise.mTask);

		awexPromise.done(new DoneCallback<Result>() {
			@Override
			public void onDone(Result result) {
				if (donePipeCallback != null) pipe(donePipeCallback.onDone(result));
				else PipedPromise.this.resolve((Result_Out) result);
			}
		}).fail(new FailCallback() {
			@Override
			public void onFail(Exception result) {
				if (failPipeCallback != null)  pipe(failPipeCallback.onFail(result));
				else PipedPromise.this.reject(result);
			}
		}).progress(new ProgressCallback<Progress>() {
			@Override
			public void onProgress(Progress progress) {
				if (progressPipeCallback != null) pipe(progressPipeCallback.onProgress(progress));
				else PipedPromise.this.notifyProgress((Progress_Out) progress);
			}
		});
	}
}