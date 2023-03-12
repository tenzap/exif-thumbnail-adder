package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class PixymetaException extends Exception {
    public PixymetaException() {}
    public PixymetaException(String txt) {
        super(txt);
    }
}
