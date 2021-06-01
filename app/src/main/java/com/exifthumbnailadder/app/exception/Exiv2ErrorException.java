package com.exifthumbnailadder.app.exception;

public class Exiv2ErrorException extends Exception  {
    public Exiv2ErrorException() {}
    public Exiv2ErrorException(String txt) {
        super(txt);
    }
}
