package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class PixymetaUnsupportedOperationException extends UnsupportedOperationException {
    public PixymetaUnsupportedOperationException() {}
    public PixymetaUnsupportedOperationException(String txt) {
        super(txt);
    }
}
