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
import androidx.test.uiautomator.UiDevice;

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
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
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
    private UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());;
    AddThumbsCommon.Dirs dir;
    Context context;

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public TestName testname = new TestName();

    @BeforeClass
    public static void clear() throws Exception {
        TestUtil.clearETA();
        TestUtil.clearDocumentsUI();
    }

    @Before
    public void init() throws Exception {
        context = getInstrumentation().getTargetContext();
        finished = false;

        dir = new AddThumbsCommon.Dirs("DCIM/test_pics", testname);
        dir.setSuffix("sg");
        uiDevice.executeShellCommand("mkdir -p " + dir.pathInStorage());
        uiDevice.executeShellCommand("rm -rf " + dir.copyPathAbsolute());
        uiDevice.executeShellCommand("mkdir -p " + dir.copyPathAbsolute());

        String files[] = {
                "/Fujifilm_FinePix_E500.jpg", //PICTURE_WITHOUT_THUMBNAIL
                "/Canon_40D.jpg", //PICTURE_WITH_THUMBNAIL
                "/Konica_Minolta_DiMAGE_Z3.jpg", //PICTURE_EXIV2_WARNING
                "/Nikon_COOLPIX_P1.jpg", //PICTURE_EXIV2_ERROR
                "/tests/67-0_length_string.jpg", //Skipping (ERROR): exiv2: Warning: Failed to decode XMP metadata. Error: XMP Toolkit error 201: Error in XMLValidator
                "/Ricoh_Caplio_RR330.jpg", //PICTURE_WITHOUT_THUMBNAIL_AS_PER_ANDROID
        };

        for (String file : files) {
            if (Build.VERSION.SDK_INT == 26) {
                // On Android O (API26), cp doesn't understand --preserve=timestamps,mode,ownership
                // But it understands --preserve=a.
                uiDevice.executeShellCommand("cp -a --preserve=a " + dir.origPathAbsolute() + file + " " + dir.copyPathAbsolute());
            } else if (Build.VERSION.SDK_INT <= 28) {
                // On Android P (API28), timestamps are not kept despite use of -a.
                // So add --preserve=timestamps,mode,ownership
                uiDevice.executeShellCommand("cp -a --preserve=timestamps,mode,ownership " + dir.origPathAbsolute() + file + " " + dir.copyPathAbsolute());
            } else {
                uiDevice.executeShellCommand("cp -a " + dir.origPathAbsolute() + file + " " + dir.copyPathAbsolute());
            }
        }
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

        // Main screen
        Screengrab.screenshot(String.format("%03d", 8));

        // Go to settings fragment
        onView(withId(R.id.SettingsFragment)).perform(click());

        // Add source folder
        TestUtil.addSourceFolder("DCIM/test_sg");

        // Set some preferences for screenshots
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("overwriteDestPic", true);
        editor.putBoolean("writeThumbnailedToOriginalFolder", false);
        editor.commit();

        // give all files access (we need it to delete folders)
        TestUtil.requestAllFilesAccess();

        // Settings screen (start)
        Screengrab.screenshot(String.format("%03d", 3));

        // Settings screen (got to bottom & scroll to "Options" category)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeUp());
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_backupOriginalPic_title)),
                        scrollTo()));
        Screengrab.screenshot(String.format("%03d", 4));

        // Settings screen (got to bottom & scroll to "Backend/Library" category)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeUp());
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_categ_libexif_settings)),
                        scrollTo()));
        Screengrab.screenshot(String.format("%03d", 5));

        // Settings screen (return to top)
        onView(allOf(withId(R.id.nav_host_fragment), hasContentDescription())).perform(swipeDown());

        // About screen
        //onView(withId(R.id.settings)).perform(pressBack());
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_about)).perform(click());
        Screengrab.screenshot(String.format("%03d", 7));

        // Return to previous screen
        Espresso.pressBack();

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

        Screengrab.screenshot(String.format("%03d", 6));

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

        Screengrab.screenshot(String.format("%03d", 1));

        // Sync screen
        deletePicture("Fujifilm_FinePix_E500.jpg");
        AddThumbsCommon.sync(context, "List");
        Screengrab.screenshot(String.format("%03d", 2));

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

    protected void deletePicture(String filename) throws IOException {
        uiDevice.executeShellCommand("rm " + dir.copyPathAbsolute() + "/" + filename);
    }

}
