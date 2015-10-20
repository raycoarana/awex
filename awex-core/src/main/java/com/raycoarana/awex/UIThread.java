package com.raycoarana.awex;

public interface UIThread {

	boolean isCurrentThread();

	void post(Runnable runnable);

}