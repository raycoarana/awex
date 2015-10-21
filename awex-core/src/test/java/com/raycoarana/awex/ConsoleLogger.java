package com.raycoarana.awex;

public class ConsoleLogger implements Logger {

    @Override
    public void v(String message) {
        System.out.println(message);
    }

    @Override
    public void e(String message, Exception ex) {
        System.err.println(message);
        ex.printStackTrace();
    }

}