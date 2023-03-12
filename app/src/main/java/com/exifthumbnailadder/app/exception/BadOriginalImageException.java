package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class BadOriginalImageException extends Exception {
    public final static String NOT_IMAGE = "Not an image.";

    public BadOriginalImageException() {

    }
    public BadOriginalImageException(String msg) {
        super(msg);
    }
}
