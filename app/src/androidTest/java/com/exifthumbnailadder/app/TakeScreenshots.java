/*
 * Copyright (C) 2021-2023 Fab Stz <fabstz-it@yahoo.fr>
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.hasContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.AllOf.allOf;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TakeScreenshots {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @BeforeClass
    public static void beforeAll() {
        //Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String appIdSuffix = "";
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            appIdSuffix = ".debug";
        }
        assertEquals("com.exifthumbnailadder.app" + appIdSuffix, appContext.getPackageName());
    }

    static {
        BuildConfig.IS_SCREENSHOTS.set(true);
    }

    @Test
    public void testTakeScreenshot() throws Exception{
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

//        // Attempt to find volume name as displayed in filePicker
//        String a = android.provider.Settings.Global.DEVICE_NAME;
//        String b = android.os.Build.MODEL;
//        String c = android.os.Build.MANUFACTURER;
//        String d = android.os.Build.PRODUCT;
//        String g = Build.HARDWARE;
//        String f = Build.DISPLAY;

        int i = 0;
        // Main screen
        Screengrab.screenshot(String.format("%03d", ++i));

        // Your custom onView...

        // Sync screen
        onView(withId(R.id.SyncFragment)).perform(click());
        Screengrab.screenshot(String.format("%03d", ++i));

        // Settings screen (start)
        onView(withId(R.id.SettingsFragment)).perform(click());
        Screengrab.screenshot(String.format("%03d", ++i));

        // Settings screen (got to bottom & scroll to "Options" category)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeUp());
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_backupOriginalPic_title)),
                        scrollTo()));
        Screengrab.screenshot(String.format("%03d", ++i));

        // Settings screen (got to bottom & scroll to "Backend/Library" category)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeUp());
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_categ_libexif_settings)),
                        scrollTo()));
        Screengrab.screenshot(String.format("%03d", ++i));

        // Settings screen (return to top)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeDown());

        // About screen
        //onView(withId(R.id.settings)).perform(pressBack());
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_about)).perform(click());
        Screengrab.screenshot(String.format("%03d", ++i));

        // Return to previous screen
        Espresso.pressBack();

        // Add source folder
        TestUtil.addSourceFolder("DCIM/sg");

        // Set some preferences for screenshots
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("overwriteDestPic", true);
        editor.putBoolean("writeThumbnailedToOriginalFolder", false);
        editor.putString("working_dir", "ThumbAdder-sg");
        editor.commit();

        // give all files access (we need it to delete folders)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !BuildConfig.FLAVOR.equals("google_play") && !MainActivity.haveAllFilesAccessPermission()) {
            TestUtil.requestAllFilesAccess();
        }

        // Delete existing WorkingDir (so that we can go to the "WorkingDirPermActivity")
        deleteDirectory(Paths.get("/storage/emulated/0/ThumbAdder-sg").toFile());

        // Go to "Add thumbnails" fragment
        onView(withId(R.id.AddThumbsFragment)).perform(click());

        // Run "Add thumbnails" which brings us to the WorkingDirPermActivity
        onView(withId(R.id.button_addThumbs)).perform(click());

//        // Give WRITE_EXTERNAL_STORAGE permissions for <R & not 'standard')
//        // Doesn't seem necessary as permission is already given in this test case
//        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && BuildConfig.FLAVOR.equals("standard"))) {
//            onView(withText(context.getString(android.R.string.ok))).perform(click());
//
//            uiElement = device.findObject(new UiSelector().clickable(true).textContains(allow));
//            try { uiElement.clickAndWaitForNewWindow(); }
//            catch (Exception e) { e.printStackTrace(); }
//        }

        Screengrab.screenshot(String.format("%03d", ++i));

        // Give permissions to the WorkingDir
        onView(withId(R.id.button_checkPermissions)).perform(click());
        TestUtil.givePermissionToWorkingDir();

        // Restart processing now that permissions to WorkingDir are given
        onView(withId(R.id.button_addThumbs)).perform(click());

        // Wait 5 sec before taking screenshot
        Thread.sleep(5000);

        Screengrab.screenshot(String.format("%03d", ++i));

        // Delete WorkingDir
        deleteDirectory(Paths.get("/storage/emulated/0/ThumbAdder-sg").toFile());
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}
