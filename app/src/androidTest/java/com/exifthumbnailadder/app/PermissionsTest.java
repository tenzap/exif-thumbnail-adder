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
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.net.ssl.ExtendedSSLSession;

@RunWith(AndroidJUnit4.class)
public class PermissionsTest {
    Context context;
    SharedPreferences prefs;
    Fragment fragment;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private IdlingResource mIdlingResource;

    @Rule
    public RepeatRule repeatRule = new RepeatRule();
    // To repeat a test, use the @Repeat(100) with the @Test annotation

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

    @Before
    public void registerIdlingResource() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = MainActivity.getIdlingResource();
            // To prove that the test fails, omit this call:
            IdlingRegistry.getInstance().register(mIdlingResource);
        });
    }

    @After
    public void unregisterIdlingResource() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    public void permissionTest(String permission, String answer) throws Exception {
        // Set idlingResourceState to false here. It is set back to true by the app
        // when it is ready for the test suite to answer the permission request dialog.
        if (PermissionManager.logIdlingResourceChanges)
            Log.d("ETA", "setIdlingResourceState: false (" + permission + ") - in PermissionsTest");
        MainActivity.setIdlingResourceState(false);
        activityScenarioRule.getScenario().onActivity(activity -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PermissionManager pm = new PermissionManager(fragment, requestPermissionLauncher);
                    pm.checkPermission(permission);
                }
            }).start();
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        // Wait until the idlingResource is idle
        Espresso.onIdle();

        switch (answer) {
            case "allow":
                TestUtil.clickPermissionAllowButton();
                assertTrue("Permissions " + permission + " should be granted.", PermissionManager.isPermissionGranted(context, permission));
                break;
            case "allow_auto":
                assertTrue("Permissions " + permission + " should be granted.", PermissionManager.isPermissionGranted(context, permission));
                break;
            case "deny":
                TestUtil.clickPermissionDenyButton();
                assertFalse("Permissions " + permission + " should be denied.", PermissionManager.isPermissionGranted(context, permission));
                break;
            case "first_deny":
                TestUtil.clickPermissionDenyButton();
                assertFalse("Permissions " + permission + " should be denied (first deny).", PermissionManager.isPermissionGranted(context, permission));
                break;
            case "second_deny":
                device.waitForWindowUpdate(context.getPackageName(), 1000);
                onView(withText(Matchers.equalToIgnoringCase(context.getString(R.string.frag1_perm_request_deny)))).perform(click());
                assertFalse("Permissions " + permission + " should be denied (2nd deny).", PermissionManager.isPermissionGranted(context, permission));
                break;
            case "ok_allow":
                device.waitForWindowUpdate(context.getPackageName(), 1000);
                onView(withText(Matchers.equalToIgnoringCase(context.getString(android.R.string.ok)))).perform(click());
                TestUtil.clickPermissionAllowButton();
                assertTrue("Permissions " + permission + " should be granted.", PermissionManager.isPermissionGranted(context, permission));
                break;
            default:
                break;
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
    public void writeExternalStorageDenyAllowTest() throws Exception {
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "first_deny");
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "ok_allow");
    }

    @Test
    public void writeExternalStorageDenyDenyTest() throws Exception {
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "first_deny");
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "second_deny");
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
    public void readExternalStorageDenyAllowTest() throws Exception {
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "first_deny");
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "ok_allow");
    }

    @Test
    public void readExternalStorageDenyDenyTest() throws Exception {
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "first_deny");
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "second_deny");
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
    public void readMediaImagesDenyAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "first_deny");
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "ok_allow");
    }

    @Test
    public void readMediaImagesDenyDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "first_deny");
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "second_deny");
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
    public void postNotificationDenyAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "first_deny");
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "ok_allow");
    }

    @Test
    public void postNotificationDenyDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "first_deny");
        permissionTest(Manifest.permission.POST_NOTIFICATIONS, "second_deny");
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

    @Test
    public void accessMediaLocationDenyAllowTest() throws Exception {
        requiresGreaterEqualThanAPI(29);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "first_deny");
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "ok_allow");
    }

    @Test
    public void accessMediaLocationDenyDenyTest() throws Exception {
        requiresGreaterEqualThanAPI(29);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "first_deny");
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "second_deny");
    }

    @Test
    public void chainedAccessMediaLocationWriteExternalStorageTest() throws Exception {
        // It is expected to have no permission dialog for the 2nd permission
        requiresGreaterEqualThanAPI(29);
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow");
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "allow_auto");
    }

    @Test
    public void chainedWriteExternalStorageAccessMediaLocationTest() throws Exception {
        // It is expected to have no permission dialog for the 2nd permission
        requiresGreaterEqualThanAPI(29);
        requiresLowerEqualThanAPI(29);
        permissionTest(Manifest.permission.WRITE_EXTERNAL_STORAGE, "allow");
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow_auto");
    }

    @Test
    public void chainedAccessMediaLocationReadExternalStorageTest() throws Exception {
        requiresGreaterEqualThanAPI(29);
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow");
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "allow_auto");
    }

    @Test
    public void chainedReadExternalStorageAccessMediaLocationTest() throws Exception {
        // It is expected to have no permission dialog for the 2nd permission
        requiresGreaterEqualThanAPI(29);
        requiresLowerEqualThanAPI(32);
        permissionTest(Manifest.permission.READ_EXTERNAL_STORAGE, "allow");
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow_auto");
    }

    @Test
    public void chainedAccessMediaLocationReadMediaImagesTest() throws Exception {
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow");
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "allow_auto");
    }

    @Test
    public void chainedReadMediaImagesAccessMediaLocationTest() throws Exception {
        // It is expected to have no permission dialog for the 2nd permission
        requiresGreaterEqualThanAPI(33);
        permissionTest(Manifest.permission.READ_MEDIA_IMAGES, "allow");
        permissionTest(Manifest.permission.ACCESS_MEDIA_LOCATION, "allow_auto");
    }

    public void requiresLowerEqualThanAPI(int api) {
        Assume.assumeTrue(Build.VERSION.SDK_INT <= api);
    }

    public void requiresGreaterEqualThanAPI(int api) {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= api);
    }
}
