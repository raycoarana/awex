package com.raycoarana.awex.exceptions;

/**
 * Copyright (c) Tuenti Technologies. All rights reserved.
 */
public class EmptyTasksException extends Exception {
	public EmptyTasksException() {
		super("Promise rejected because the method has been called with " +
				"an empty task collection.");
	}
}
