package com.exifthumbnailadder.app;

import android.app.Application;

public class MainApplication extends Application {
    public static final String TAG = "ETALog";
    public static final boolean enableLog = BuildConfig.BUILD_TYPE.equals("debug") ? true : false;

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