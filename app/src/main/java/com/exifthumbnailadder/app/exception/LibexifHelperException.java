package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class LibexifHelperException extends Exception {
    public LibexifHelperException() {}
    public LibexifHelperException(String txt) {
        super(txt);
    }
}
