package com.exifthumbnailadder.app;

import android.app.Application;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    static {
        try {
            System.loadLibrary("exifThumbnailAdderHelpers");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}