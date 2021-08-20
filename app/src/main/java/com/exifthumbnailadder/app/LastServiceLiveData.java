package com.exifthumbnailadder.app;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;

public class LastServiceLiveData extends LiveData<String> {
    private static LastServiceLiveData sInstance;

    @MainThread
    public static LastServiceLiveData get() {
        if (sInstance == null) {
            sInstance = new LastServiceLiveData();
        }
        return sInstance;
    }

    private LastServiceLiveData() {
    }

    public void setLastService(String s) {
        postValue(s);
    }
}
