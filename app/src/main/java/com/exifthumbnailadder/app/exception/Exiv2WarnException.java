package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class Exiv2WarnException extends Exception {
    public Exiv2WarnException() {}
    public Exiv2WarnException(String txt) {
        super(txt);
    }
}
