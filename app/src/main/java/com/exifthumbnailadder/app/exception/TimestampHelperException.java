package com.exifthumbnailadder.app.exception;

public class TimestampHelperException extends Exception {
    public TimestampHelperException(Throwable err) {
        super(err);
    }
    public TimestampHelperException(String msg) {
        super(msg);
    }
}