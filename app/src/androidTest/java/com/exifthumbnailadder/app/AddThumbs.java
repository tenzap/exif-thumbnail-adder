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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AddThumbs {
    Context context;
    SharedPreferences prefs;

    @Rule
    public TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @AfterClass
    public static void resetPerm() throws Exception {
        TestUtil.resetETAPermissions();
    }

    // https://stackoverflow.com/a/54203607
    @BeforeClass
    public static void dismissANRSystemDialog() throws UiObjectNotFoundException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int resId = context.getResources().getIdentifier("wait", "string", "android");
        String wait = context.getResources().getString(resId);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject waitButton = device.findObject(new UiSelector().textMatches("(?i)" + wait));
        if (waitButton.exists()) {
            waitButton.click();
        }
    }

    @BeforeClass
    public static void clear() throws Exception {
        TestUtil.clearETA();
        TestUtil.clearDocumentsUI();
    }

    @Test
    public void addThumbs() throws Exception {
        // Go to Settings
        TestUtil.openSettingsFragment();

        // Add Folder in settings
        TestUtil.addSourceFolder("DCIM/test_pics");

        // Check that folder is in the list
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        assertEquals(1, inputDirs.size());
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3ADCIM%2Ftest_pics/document/primary%3ADCIM%2Ftest_pics", inputDirs.get(0).toString());

        // give all files access (we need it to delete folders)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !BuildConfig.FLAVOR.equals("google_play") && !MainActivity.haveAllFilesAccessPermission()) {
            TestUtil.requestAllFilesAccess();
        }

        // Go to "Add thumbnails" fragment
        onView(withId(R.id.AddThumbsFragment)).perform(click());

        // Click "Add thumbnails" button
        onView(withId(R.id.button_addThumbs)).perform(click());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            TestUtil.clickPermissionAllowButton();
            // Click "Add thumbnails" button again
            onView(withId(R.id.button_addThumbs)).perform(click());
        } else {
            if (BuildConfig.FLAVOR.equals("google_play")) {
                TestUtil.clickPermissionAllowButton();
                // Click "Add thumbnails" button again
                onView(withId(R.id.button_addThumbs)).perform(click());
            } else {
            }
        }

        // The WorkingDirPermActivity has now launched.
        // Create & Give permissions to the WorkingDir
        onView(withId(R.id.button_checkPermissions)).perform(click());
        TestUtil.givePermissionToWorkingDir();

        // We are back to the MainAcitivity / Add Thumbs fragment
        // Click on "Add Thumbs" button to really start processing now that permissions to WorkingDir are given
        onView(withId(R.id.button_addThumbs)).perform(click());

        // Wait 5 sec
        Thread.sleep(5000);
    }

}
