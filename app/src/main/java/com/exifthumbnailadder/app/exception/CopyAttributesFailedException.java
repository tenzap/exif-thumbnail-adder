package com.exifthumbnailadder.app.exception;

public class CopyAttributesFailedException extends Exception {
    public CopyAttributesFailedException(Throwable err) {
        super(err);
    }
    public CopyAttributesFailedException(String msg) {
        super(msg);
    }
}