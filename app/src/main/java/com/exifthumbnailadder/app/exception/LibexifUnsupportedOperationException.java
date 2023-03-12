package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class LibexifUnsupportedOperationException extends UnsupportedOperationException {
    public LibexifUnsupportedOperationException() {}
    public LibexifUnsupportedOperationException(String txt) {
        super(txt);
    }
}
