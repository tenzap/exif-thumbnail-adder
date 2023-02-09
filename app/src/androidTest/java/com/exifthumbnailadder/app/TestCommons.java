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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestCommons {
    static {
        BuildConfig.IS_TESTING.set(true);
    }

    @Rule
    public TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    // https://stackoverflow.com/a/54203607
    @BeforeClass
    public static void dismissANRSystemDialog() throws UiObjectNotFoundException {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        UiObject dialogTitle = device.findObject(new UiSelector().resourceId("android:id/alertTitle"));
        UiObject closeButton = device.findObject(new UiSelector().resourceId("android:id/aerr_close"));
        UiObject waitButton = device.findObject(new UiSelector().resourceId("android:id/aerr_wait"));

        if (dialogTitle.exists()) {
            Log.d("ETATest", "ANR Dialog open.");
            Log.d("ETATest", "Title: " + dialogTitle.getText());

            if (dialogTitle.getText().startsWith("System UI")) {
                // On API 33, when ANR dialog is 'System UI is not responding',
                // wait doesn't seem sufficient because the ANR dialog reappears. So click 'Close app'
                Log.d("ETATest", "Before check if closeButton exists");
                if (closeButton.exists()) {
                    Log.d("ETATest", "closeButton - Before click");
                    closeButton.click();
                    Log.d("ETATest", "closeButton - After click");
                }
            } else {
                Log.d("ETATest", "Before check if waitButton exists");
                if (waitButton.exists()) {
                    Log.d("ETATest", "waitButton - Before click");
                    waitButton.click();
                    Log.d("ETATest", "waitButton - After click");
                }
            }
            Log.d("ETATest", "Before waitForIdle");
            device.waitForIdle();
            Log.d("ETATest", "After waitForIdle");
            Log.d("ETATest", "ANR Dialog should now be closed.");
        } else {
            Log.d("ETATest", "No ANR Dialog open. Continuing.");
        }
    }

}
