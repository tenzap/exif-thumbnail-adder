/*
 * Copyright (C) 2021 Fab Stz <fabstz-it@yahoo.fr>
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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.hasContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.AllOf.allOf;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Paths;

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
public class ExampleInstrumentedTest {
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
        assertEquals("com.exifthumbnailadder.app", appContext.getPackageName());
    }

    @Test
    public void testTakeScreenshot() {
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

        // Settings screen (got to bottom)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeUp());
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
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String allow = "", allowAccessTo = "";
        String select = "", save = "";
        int resId;

        try {
            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r30:packages/apps/DocumentsUI/res/values/strings.xml
            PackageManager manager = context.getPackageManager();
            Resources resources = manager.getResourcesForApplication("com.android.documentsui");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resId = resources.getIdentifier("allow", "string", "com.android.documentsui");
                allow = resources.getString(resId);
                resId = resources.getIdentifier("open_tree_button", "string", "com.android.documentsui");
                allowAccessTo = resources.getString(resId);
                allowAccessTo = allowAccessTo.replaceFirst(" \"%1\\$s\"", ""); //remove "%1$s"
            } else {
                resId = resources.getIdentifier("button_select", "string", "com.android.documentsui");
                select = resources.getString(resId);
            }
            resId = resources.getIdentifier("menu_save", "string", "com.android.documentsui");
            save = resources.getString(resId);

        } catch (Exception e) { e.printStackTrace(); }

        UiObject uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)"+context.getString(R.string.settings_button_add_dir)));
        try { uiElement.clickAndWaitForNewWindow(); }
        catch (Exception e) { e.printStackTrace(); }

        String volumeNameInFilePicker = Build.MODEL;

        int iterations_count = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ? 2 : 1;
        for (int j=0; j<iterations_count; j++) {
            // Need to do it twice to be sure to catch the sd card. Sometimes it fails to do so.
            UiObject drawer = device.findObject(new UiSelector().resourceId("com.android.documentsui:id/drawer_layout"));
            try {
                drawer.swipeRight(50);
                drawer.waitForExists(250);
            } catch (Exception e) { e.printStackTrace(); }

            //uiElement = device.findObject(new UiSelector().textMatches("(?i).*Virtual.*"));
            uiElement = device.findObject(new UiSelector().textMatches("(?i)"+volumeNameInFilePicker));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        }

        uiElement = device.findObject(new UiSelector().textContains("DCIM"));
        try { uiElement.clickAndWaitForNewWindow(); }
        catch (Exception e) { e.printStackTrace(); }

        uiElement = device.findObject(new UiSelector().textContains("sg"));
        try { uiElement.clickAndWaitForNewWindow(); }
        catch (Exception e) { e.printStackTrace(); }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(allowAccessTo));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }

            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + allow));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + select));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        }

        // Set some preferences for screenshots
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("overwriteDestPic", true);
        editor.putBoolean("writeThumbnailedToOriginalFolder", false);
        editor.putString("working_dir", "ThumbAdder-sg");
        editor.commit();

        // Delete existing WorkingDir (so that we can go to the "WorkingDirPermActivity")
        deleteDirectory(Paths.get("/storage/emulated/0/ThumbAdder-sg").toFile());

        // Go to "Add thumbnails" fragment
        onView(withId(R.id.FirstFragment)).perform(click());

        // Run "Add thumbnails" which brings us to the WorkingDirPermActivity
        onView(withId(R.id.button_addThumbs)).perform(click());
        Screengrab.screenshot(String.format("%03d", ++i));

        // Give permissions to the WorkingDir
        onView(withId(R.id.button_checkPermissions)).perform(click());

        uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)"+context.getString(R.string.frag1_button_start_processing)));
        try { uiElement.clickAndWaitForNewWindow(); }
        catch (Exception e) { e.printStackTrace(); }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + save));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(allowAccessTo));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + allow));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + save));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + select));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        }

        // Restart processing now that permissions to WorkingDir are given
        onView(withId(R.id.button_addThumbs)).perform(click());

        // Wait 5 sec before taking screenshot
        uiElement.waitForExists(5000);
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
