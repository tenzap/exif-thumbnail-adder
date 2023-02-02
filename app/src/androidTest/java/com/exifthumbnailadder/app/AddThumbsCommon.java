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
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class AddThumbsCommon {
    Context context;
    SharedPreferences prefs;
    public Dirs dir;
    public boolean finished;

    @Rule
    public TestName testname = new TestName();

    @Rule
    public TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public IntentsRule intentsTestRule = new IntentsRule();

    // https://stackoverflow.com/a/54203607
    @BeforeClass
    public static void dismissANRSystemDialog() throws UiObjectNotFoundException {
        Context context = getInstrumentation().getTargetContext();
        int resId = context.getResources().getIdentifier("wait", "string", "android");
        String wait = context.getResources().getString(resId);
        UiDevice device = UiDevice.getInstance(getInstrumentation());

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

    @Before
    public void init() throws Exception {
        context = getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());

        finished = false;

        dir = new Dirs("DCIM/test_pics");
        uiDevice.executeShellCommand("mkdir -p " + dir.pathInStorage());
        uiDevice.executeShellCommand("rm -rf " + dir.copyPathAbsolute());
        if (Build.VERSION.SDK_INT == 26) {
            // On Android O (API26), cp doesn't understand --preserve=timestamps,mode,ownership
            // But it understands --preserve=a.
            uiDevice.executeShellCommand("cp -a --preserve=a " + dir.origPathAbsolute() + " " + dir.copyPathAbsolute());
        } else if (Build.VERSION.SDK_INT <= 28) {
            // On Android P (API28), timestamps are not kept despite use of -a.
            // So add --preserve=timestamps,mode,ownership
            uiDevice.executeShellCommand("cp -a --preserve=timestamps,mode,ownership " + dir.origPathAbsolute() + " " + dir.copyPathAbsolute());
        } else {
            uiDevice.executeShellCommand("cp -a " + dir.origPathAbsolute() + " " + dir.copyPathAbsolute());
        }
    }

    @After
    public void saveOutput() throws IOException {
        moveOnFSWithShell(dir.workingDir("ThumbAdder"), dir.storageBasePathAbsolute());
        moveOnFSWithShell(dir.workingDir("JustSomething"), dir.storageBasePathAbsolute());
        moveOnFSWithShell(dir.copyPathAbsolute(), dir.pathInStorage());
    }

    public void addThumbs() throws Exception {
        addThumbs(null);
    }

    public void addThumbs(HashMap opts) throws Exception {
        // Go to Settings
        TestUtil.openSettingsFragment();

        // Add Folder in settings
        TestUtil.addSourceFolder(dir.copyPath());

        // Check that folder is in the list
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        assertEquals(1, inputDirs.size());
        String expectedValue = "content://com.android.externalstorage.documents/tree/primary%3A" + dir.copyForUri() + "/document/primary%3A" + dir.copyForUri();
        assertEquals("Not exactly one selected source dir", expectedValue, inputDirs.get(0).toString());

        if (opts != null &&
                opts.containsKey("all_files_access") &&
                opts.get("all_files_access").equals(new Boolean(true))) {
            TestUtil.requestAllFilesAccess();
        } else {
            // TODO: revokeAllFilesAccess https://stackoverflow.com/q/75102412/15401262addThumbsSettingsUpdateInSourceOffWithDestOverwrite
            // Cannot revoke for now, so fail test if All Files acccess is enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                assertFalse("All Files access is granted. Should not be in these tests.", PermissionManager.hasAllFilesAccessPermission());
            }
        }

        // Go to "Add thumbnails" fragment
        onView(withId(R.id.AddThumbsFragment)).perform(click(click()));

        // Click "Add thumbnails" button
        onView(withId(R.id.button_addThumbs)).perform(click());

        // ATTENTION: This below requires to be on a clean app (where permissions have been reset)
        // Same condition as in AddThumbsFragment.addThumbsUsingTreeUris() to trigger the WRITE_EXTERNAL_STORAGE permission
        if (Build.VERSION.SDK_INT <= 29 || !prefs.getBoolean("useSAF", true)) {
            // Trigger only if WRITE_EXTERNAL_STORAGE is not granted yet
            if (!PermissionManager.hasWriteExternalStorage(context)) {
                TestUtil.clickPermissionAllowButton();
            }
        }

        // Wait until 'WorkingDirPermActivity' has launched
        Intents.intended(allOf(hasComponent(WorkingDirPermActivity.class.getName())));

        // The WorkingDirPermActivity has now launched.
        // Create & Give permissions to the WorkingDir
        // For this use scrollTo(). See: https://stackoverflow.com/a/30390176/15401262
        // Former method with swipeUp sometimes failed with
        // androidx.test.espresso.NoMatchingViewException: No views in hierarchy found matching: view.getId() is <2131362157/com.exifthumbnailadder.app.debug:id/permScrollView>
        onView(withId(R.id.button_checkPermissions)).perform(scrollTo(), click());

        TestUtil.givePermissionToWorkingDir();

        int runs = 1;
        if (opts != null &&
                opts.containsKey("rerun_processing") &&
                opts.get("rerun_processing").equals(new Boolean(true))) {
            runs = 2;
        }

        for (int i = 0; i < runs; i++) {
            finished = false;
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

            // On API33 (and when target SDK <= 32), the user is asked to give Notification permission
            if (i == 0 && Build.VERSION.SDK_INT >=33) {
                TestUtil.clickPermissionAllowButton();
            }

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
        }

        String log = getText(withId(R.id.textview_log));
        writeTextToFile("log.txt", log);

        assertTrue("Processing couldn't finish (timeout?)", finished);
    }

    public class Dirs {

        public final String ROOT = "/storage/emulated/0";
        public final String OUTPUT_STORAGE_ROOT = "/data/local/tmp/test_output/" + Build.VERSION.SDK_INT;

        Path path;

        public Dirs(String path) {
            this.path = Paths.get(path);
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

        public String origPathAbsolute() {
            return ROOT + "/" + path() + "/" + orig();
        }

        public String copyPathAbsolute() {
            return ROOT + "/" + path() + "/" + copy();
        }

        public String storageBasePathAbsolute() {
            return OUTPUT_STORAGE_ROOT + "/" + suffix();
        }

        public String pathInStorage() {
            return storageBasePathAbsolute() + "/" + path();
        }

        public String workingDir(String dir) {
            return ROOT + "/" + dir;
        }
    }

    String getText(final Matcher<View> matcher) {
        final String[] stringHolder = {null};
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
                TextView tv = (TextView) view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    private void writeTextToFile(String filename, String data) throws IOException {
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
        Uri srcDirUri = inputDirs.get(0);
        Uri logFile = null;

        if (srcDirUri.getScheme().equals("file")) {
            logFile = Uri.fromFile(new File(srcDirUri.getPath() + File.separator + filename));
        }

        DocumentFile outputFileDf = DocumentFile.fromTreeUri(context, srcDirUri).findFile(filename);
        try {
            if (outputFileDf != null) {
                logFile = outputFileDf.getUri();
            } else {
                logFile = DocumentsContract.createDocument(
                        context.getContentResolver(),
                        srcDirUri,
                        "text/plain",
                        filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        OutputStream outputStream = context.getContentResolver().openOutputStream(logFile);
        byte[] bytes = data.getBytes();
        outputStream.write(bytes);
        outputStream.close();

        moveOnFSWithShell(dir.copyPathAbsolute() + "/" + filename, dir.storageBasePathAbsolute());
    }

    void syncList() throws Exception {
        // Go to Sync Fragment
        TestUtil.openSyncFragment();

        finished = false;
        // Register BroadcastReceiver of the signal saying that processing is finished
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED":
                        finished = true;
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED");
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter);

        // Click "List files" button
        onView(withId(R.id.sync_button_list_files)).perform(click());

        // Wait until processing is finished or has hit timeout (duration is in ms)
        long max_duration = 300000;
        long timeout = System.currentTimeMillis() + max_duration;
        while (!finished && System.currentTimeMillis() < timeout) {
            Thread.sleep(1000);
        }

        // Stop processing if not finished
        if (!finished) {
            try {
                onView(withId(R.id.sync_button_stop)).perform(click());
            } catch (PerformException e) {
                // This exception happens when button_stopProcess is not in the view.
                e.printStackTrace();
            }
        }

        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(receiver);

        String log = getText(withId(R.id.sync_textview_log));
        writeTextToFile("sync_log.txt", log);
    }

    void syncDelete() throws Exception {
        // Go to Sync Fragment
        TestUtil.openSyncFragment();

        finished = false;
        // Register BroadcastReceiver of the signal saying that processing is finished
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED":
                        finished = true;
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED");
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter);

        // Click "Delete" button
        onView(withId(R.id.sync_button_del_files)).perform(click());

        // Wait until processing is finished or has hit timeout (duration is in ms)
        long max_duration = 300000;
        long timeout = System.currentTimeMillis() + max_duration;
        while (!finished && System.currentTimeMillis() < timeout) {
            Thread.sleep(1000);
        }

        // Stop processing if not finished
        if (!finished) {
            try {
                onView(withId(R.id.sync_button_stop)).perform(click());
            } catch (PerformException e) {
                // This exception happens when button_stopProcess is not in the view.
                e.printStackTrace();
            }
        }

        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(receiver);

        String log = getText(withId(R.id.sync_textview_log));
        writeTextToFile("sync_log.txt", log);
    }

    protected void deletePicture(String filename) throws IOException {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.executeShellCommand("rm " + dir.copyPathAbsolute() + "/" + filename);
    }

    private void moveOnFSWithShell(String source, String destination) throws IOException {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        // On API <= 28, "mv" doesn't preserve the timestamps, so use tar instead
        // NB: Using tar with pipe doesn't seem to work 'tar -C <source> -cf - . | tar -C <destination> -xf -'
        // So use an intermediate file.
        if (Build.VERSION.SDK_INT <= 28) {
            device.executeShellCommand("rm -f /data/local/tmp/tmp.tar");

            device.executeShellCommand("tar -C " + Paths.get(source).getParent() + " -cf /data/local/tmp/tmp.tar " + Paths.get(source).getFileName());

            device.executeShellCommand("mkdir -p " + destination);
            device.executeShellCommand("tar -C " + destination + " -xf /data/local/tmp/tmp.tar");

            device.executeShellCommand("rm -rf " + source);
            device.executeShellCommand("rm -f /data/local/tmp/tmp.tar");
        } else {
            device.executeShellCommand("mv " + source + " " + destination);
        }
    }
}
