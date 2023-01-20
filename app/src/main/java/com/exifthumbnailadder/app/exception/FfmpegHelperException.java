package com.exifthumbnailadder.app.exception;

import androidx.annotation.Keep;

@Keep
public class FfmpegHelperException extends Exception {
    public FfmpegHelperException() {}
    public FfmpegHelperException(String txt) {
        super(txt);
    }
}
