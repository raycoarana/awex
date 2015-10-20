package com.raycoarana.awex;

public interface Logger {

	void v(String message);

	void e(String message, Exception ex);
}