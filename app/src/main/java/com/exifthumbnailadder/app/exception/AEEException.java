package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class AEEException extends Exception {
    public AEEException() {}
    public AEEException(String txt) {
        super(txt);
    }
}
