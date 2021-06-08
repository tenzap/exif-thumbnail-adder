package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class LibexifException extends Exception {
    public LibexifException() {}
    public LibexifException(String txt) {
        super(txt);
    }
}
