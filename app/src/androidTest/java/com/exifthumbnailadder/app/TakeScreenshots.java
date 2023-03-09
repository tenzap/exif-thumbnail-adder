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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

import org.junit.After;
import org.junit.Before;
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
public class TakeScreenshots {
    private IdlingResource mIdlingResource;
    private IdlingResource mWorkingDirPermIdlingResource;
    public boolean finished;

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

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
        TestUtil.requestAllFilesAccess();

        // Delete existing WorkingDir (so that we can go to the "WorkingDirPermActivity")
        deleteDirectory(Paths.get("/storage/emulated/0/ThumbAdder-sg").toFile());

        // Go to "Add thumbnails" fragment
        onView(withId(R.id.AddThumbsFragment)).perform(click());

        finished = false;
        // Register BroadcastReceiver of the signal saying that processing is finished
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.ADD_THUMBS_FRAGMENT_FINISHED":
                        finished = true;
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.ADD_THUMBS_FRAGMENT_FINISHED");
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter);

        // Run "Add thumbnails" which brings us to the WorkingDirPermActivity
        onView(withId(R.id.button_addThumbs)).perform(click());

        for (String perm : PermissionManager.getRequiredPermissions(prefs)) {
            if (!PermissionManager.isPermissionGranted(context, perm)) {
                if (perm.equals(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE))
                    continue;
                if (perm.equals(android.Manifest.permission.ACCESS_MEDIA_LOCATION)) {
                    if (PermissionManager.isPermissionGranted(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            PermissionManager.isPermissionGranted(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            PermissionManager.isPermissionGranted(context, Manifest.permission.READ_MEDIA_IMAGES))
                        continue;
                }
                TestUtil.clickPermissionAllowButton();
            }
        }

        Screengrab.screenshot(String.format("%03d", ++i));

        // Give permissions to the WorkingDir
        IdlingRegistry.getInstance().register(mWorkingDirPermIdlingResource);
        TestUtil.workingDirPermActivityCheckPermission();
        IdlingRegistry.getInstance().unregister(mWorkingDirPermIdlingResource);

        Log.d("ETATest", "Before givePermissionToWorkingDir()");
        TestUtil.givePermissionToWorkingDir();
        Log.d("ETATest", "After givePermissionToWorkingDir()");


        // Wait until processing is finished or has hit timeout (duration is in ms)
        long max_duration = 20 * 60 * 1000;
        long timeout = System.currentTimeMillis() + max_duration;
        while (!finished && System.currentTimeMillis() < timeout) {
            Thread.sleep(1000);
        }

        // Stop processing if not finished (ie. in the case timeout was hit)
        if (!finished) {
            try {
                onView(withId(R.id.button_stopProcess)).perform(click());
            } catch (PerformException e) {
                // This exception happens when button_stopProcess is not in the view.
                e.printStackTrace();
            }
        }

        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(receiver);

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

    @Before
    public void registerIdlingResource() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = MainActivity.getIdlingResource();
            // To prove that the test fails, omit this call:
            IdlingRegistry.getInstance().register(mIdlingResource);

            mWorkingDirPermIdlingResource = MainActivity.getWorkingDirPermIdlingResource();
        });
    }

    @After
    public void unregisterIdlingResource() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }
}
