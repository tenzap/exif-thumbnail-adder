/*
 * Copyright (C) 2023 Fab Stz <fabstz-it@yahoo.fr>
 *
 * This file is part of Exif Thumbnail Adder. An android app that adds
 * thumbnails in EXIF tags of your pictures that don't have one yet.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.exifthumbnailadder.app;

import androidx.annotation.WorkerThread;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

// Inspired from https://medium.com/jumio/how-to-record-screen-captures-during-ui-tests-on-android-67ba5dedb7e1
public class TestDataCollectionRule extends TestWatcher {

    private final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String ScPath = "/data/local/tmp/screenrecords/";

    @WorkerThread
    public void startScreenRecord(String fileName) throws IOException {
//        try {
        uiDevice.executeShellCommand("mkdir -p " + ScPath);
        uiDevice.executeShellCommand("screenrecord --bit-rate 5M --bugreport " + ScPath + fileName + ".mp4");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @WorkerThread
    public void stopScreenRecord() throws IOException {
//        try {
        uiDevice.executeShellCommand("pkill -2 screenrecord");
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    private String getTestName(Description description) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String timeStamp = simpleDateFormat.format(Calendar.getInstance().getTime());
        return timeStamp + "_" + BuildConfig.FLAVOR + "_" + description.getMethodName();
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);
        new Thread() {
            public void run() {
                super.run();
                try { startScreenRecord(getTestName(description)); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }.start();
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        new Thread() {
            public void run() {
                super.run();
                try { stopScreenRecord(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }.start();
    }

}
