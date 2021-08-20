package com.exifthumbnailadder.app;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

public class SyncLogLiveData extends LiveData<SpannableStringBuilder> {
    private static SyncLogLiveData sInstance;
    private final static SpannableStringBuilder log = new SpannableStringBuilder("");

    @MainThread
    public static SyncLogLiveData get() {
        if (sInstance == null) {
            sInstance = new SyncLogLiveData();
        }
        return sInstance;
    }

    private SyncLogLiveData() {
    }

    public void appendLog(String text) {
        if (enableLog) Log.i(TAG, text);
        log.append(text);
        postValue(log);
    }

    public void appendLog(Spanned text) {
        if (enableLog) Log.i(TAG, text.toString());
        log.append(text);
        postValue(log);
    }

    public void clear() {
        log.clear();
    }
}
