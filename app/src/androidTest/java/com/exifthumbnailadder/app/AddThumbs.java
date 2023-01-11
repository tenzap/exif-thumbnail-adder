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
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(AndroidJUnit4.class)
public class AddThumbs {
    Context context;
    SharedPreferences prefs;

    @Rule public TestName testname = new TestName();
    public Dirs dir;
    public boolean finished;

    @Rule
    public TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());

        finished = false;

        dir = new Dirs("DCIM/test_pics", "ThumbAdder");
        uiDevice.executeShellCommand("mkdir -p " + dir.pathInStorage());
        uiDevice.executeShellCommand("cp -a " + dir.origFromRoot() + " " + dir.copyFromRoot());
    }

    @AfterClass
    public static void resetPerm() throws Exception {
        TestUtil.resetETAPermissions();
    }

    @After
    public void saveOutput() throws IOException {
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());
        uiDevice.executeShellCommand("mv " + dir.workingDir() + " " + dir.storageTestRoot());
        uiDevice.executeShellCommand("mv " + dir.copyFromRoot() + " " + dir.pathInStorage());
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
        TestUtil.addSourceFolder(dir.copyPath());

        // Check that folder is in the list
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        assertEquals(1, inputDirs.size());
        String expectedValue = "content://com.android.externalstorage.documents/tree/primary%3A"+ dir.copyForUri() + "/document/primary%3A" + dir.copyForUri();
        assertEquals(expectedValue, inputDirs.get(0).toString());

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
        // For this: swipeUp & click on button
        onView(withId(R.id.permScrollView)).perform(swipeUp());
        onView(withId(R.id.button_checkPermissions)).perform(click());
        TestUtil.givePermissionToWorkingDir();

        // Register BroadcastReceiver of the signal saying that processing is finished
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_FINISHED":
                        finished = true;
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_FINISHED");
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter);

        // We are back to the MainActivity / Add Thumbs fragment
        // Click on "Add Thumbs" button to really start processing now that permissions to WorkingDir are given
        onView(withId(R.id.button_addThumbs)).perform(click());

        // Wait until processing is finished or has hit timeout (duration is in ms)
        long max_duration = 1800000;
        long timeout = System.currentTimeMillis() + max_duration;
        while (!finished && System.currentTimeMillis() < timeout) {
            Thread.sleep(1000);
        }

        // Stop processing if not finished
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

        String log = getText(withId(R.id.textview_log));
        writeToFile("log.txt", log);

        assertTrue(finished);
    }
    public class Dirs {

        public final String ROOT = "/storage/emulated/0";
        public final String OUTPUT_STORAGE_ROOT = "/data/local/tmp/test_output/" + Build.VERSION.SDK_INT;

        Path path;
        String workingDir;

        public Dirs(String path, String workingDir) {
            Log.e("ETA", "eerere");
            this.path = Paths.get(path);
            this.workingDir = workingDir;
        }
        public String copyPath() {
            return path() + "/" + copy();

        }

        public String path() {
            // DCIM
            return path.getParent().toString();
        }

        public String orig() {
            // test_pics
            return path.getFileName().toString();
        }

        public String copy() {
            // test_<FLAVOR>_<TESTNAME>
            return "test_" + suffix();
        }

        public String suffix() {
            // <FLAVOR>_<TESTNAME>
            return BuildConfig.FLAVOR + "_" + testname.getMethodName();
        }

        public String copyForUri() {
            // DCIM/test_<FLAVOR>_<TESTNAME> -> DCIM%2Ftest_<FLAVOR>_<TESTNAME>
            return path().replaceAll("/", "%2F") + "%2F" + copy();
        }

        public String origFromRoot() {
            return ROOT + "/" + path() + "/" + orig();
        }
        public String copyFromRoot() {
            return ROOT + "/" + path() + "/" + copy();
        }
        public String storageTestRoot() {
            return OUTPUT_STORAGE_ROOT + "/" + suffix();
        }

        public String pathInStorage() {
            return storageTestRoot() + "/" + path();
        }
        public String workingDir() {
            return ROOT + "/" + workingDir;
        }
    }

    String getText(final Matcher<View> matcher) {
        final String[] stringHolder = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    private void writeToFile(String filename, String data) throws IOException {
        File file = new File(dir.copyFromRoot() + "/" + filename);
        try(FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            //convert string to byte array
            byte[] bytes = data.getBytes();
            //write byte array to file
            bos.write(bytes);
            bos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.executeShellCommand("mv " + dir.copyFromRoot() + "/" + filename + " " + dir.storageTestRoot());
    }
}
