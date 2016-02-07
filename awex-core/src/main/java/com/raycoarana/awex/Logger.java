package com.raycoarana.awex;

public interface Logger {

    boolean isEnabled();

    void v(String message);

    void e(String message, Exception ex);
}