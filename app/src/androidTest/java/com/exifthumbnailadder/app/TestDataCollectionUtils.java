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

import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class TestDataCollectionUtils {

    @Rule
    public static TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    protected static final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private static final String ScPath = "/data/local/tmp/screenrecords/" + android.os.Build.VERSION.SDK_INT;

    @WorkerThread
    public static void startScreenRecord(String fileName) {
        try {
            uiDevice.executeShellCommand("mkdir -p " + ScPath);
            uiDevice.executeShellCommand("screenrecord --size 1280x720 --bit-rate 1M --bugreport " + ScPath + "/" + fileName + ".mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    public static void stopScreenRecord() {
        try {
            uiDevice.executeShellCommand("pkill -2 screenrecord");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    public static void storeLogcat(String fileName, String date) {
        String logcatOutputFilename = ScPath + "/" + fileName + ".logcat.txt";
        String logcatCommand = "logcat -t " + date + " -f " + logcatOutputFilename;
        Log.d("ETATest", "logcatCommand: " + logcatCommand);

        try {
            String output = uiDevice.executeShellCommand(logcatCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
