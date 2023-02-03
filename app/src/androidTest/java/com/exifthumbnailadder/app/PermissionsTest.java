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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PermissionsTest {
    Context context;
    SharedPreferences prefs;
    Fragment fragment;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Rule
    public TestName testname = new TestName();

    @Rule
    public TestDataCollectionRule testDataCollectionRule = new TestDataCollectionRule();

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() throws Exception {
        activityScenarioRule.getScenario().onActivity(activity -> {
            fragment = activity.getForegroundFragment();
            if (fragment instanceof AddThumbsFragment)
                requestPermissionLauncher = ((AddThumbsFragment) fragment).getRequestPermissionLauncher();
        });
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.executeShellCommand("mkdir -p /storage/emulated/0/DCIM/test_pics");
        TestUtil.clearETA();
        TestUtil.clearDocumentsUI();
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

    public void permissionTest(String permission, String answer) throws Exception {
        new Thread() {
            @Override
            public void run() {
                activityScenarioRule.getScenario().onActivity(activity -> {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PermissionManager pm = new PermissionManager(fragment, requestPermissionLauncher);
                            pm.checkPermission(permission);
                        }
                    }).start();
                });
            }
        }.start();

        String permPackage;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permPackage = "com.android.packageinstaller";
        } else {
            permPackage = "com.android.permissioncontroller";
        }

        // Wait until permission controller is displayed
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.waitForWindowUpdate(permPackage, 5000);

        if (answer.equals("allow")) {
            TestUtil.clickPermissionAllowButton();
            assertTrue("Permissions " + permission + " should be granted.", PermissionManager.isPermissionGranted(context, permission));
        } else if (answer.equals("deny")) {
            TestUtil.clickPermissionDenyButton();
            assertFalse("Permissions " + permission + " should be denied.", PermissionManager.isPermissionGranted(context, permission));
        }
    }

    @Test
    public void writeExternalStorageAllowTest() throws Exception {
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "allow");
    }

    @Test
    public void writeExternalStorageDenyTest() throws Exception {
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "deny");
    }

    @Test
    public void readExternalStorageAllowTest() throws Exception {
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "allow");
    }

    @Test
    public void readExternalStorageDenyTest() throws Exception {
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "deny");
    }

    @Test
    public void readMediaImagesAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "allow");
    }

    @Test
    public void readMediaImagesDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "deny");
    }

    @Test
    public void postNotificationAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "allow");
    }

    @Test
    public void postNotificationDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "deny");
    }

    @Test
    public void accessMediaLocationAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(29);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow");
    }

    @Test
    public void accessMediaLocationDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(29);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "deny");
    }

    public void requiresLowerEqualThanAPI(int api) {
        Assume.assumeTrue(Build.VERSION.SDK_INT <= api);
    }

    public void requiresGreaterEqualThanAPI(int api) {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= api);
    }

}
