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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.Matchers.allOf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TestUtil {

    private static boolean srcAdded = false;
    private static UiDevice device = UiDevice.getInstance(getInstrumentation());

    public static void requestAllFilesAccess() throws Exception {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !MainActivity.haveAllFilesAccessPermission()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                ! Environment.isExternalStorageManager() &&
                ! Environment.isExternalStorageLegacy() &&
                PermissionManager.manifestHasMANAGE_EXTERNAL_STORAGE(InstrumentationRegistry.getInstrumentation().getTargetContext())) {

            UiDevice device = UiDevice.getInstance(getInstrumentation());

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            String permit_manage_external_storage;
            int resId;

            PackageManager manager = context.getPackageManager();

            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/res/values/strings.xml
            Resources resources = manager.getResourcesForApplication("com.android.settings");
            resId = resources.getIdentifier("permit_manage_external_storage", "string", "com.android.settings");
            permit_manage_external_storage = resources.getString(resId);

            onView(withText(R.string.pref_allFilesAccess_title)).perform(click());

            UiObject uiElement2 = device.findObject(new UiSelector().textMatches("(?i)" + permit_manage_external_storage));
            uiElement2.click();
            device.pressBack();
        }
    }

    public static String getSdCardNameInFilePicker() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager manager = context.getPackageManager();
        int resId = 0;
        Resources resources = null;
        // Identifier names are taken here:
        // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/res/values/strings.xml
        resources = manager.getResourcesForApplication("com.android.settings");
        resId = resources.getIdentifier("sdcard_setting", "string", "com.android.settings");
        return resources.getString(resId);
    }

    public static void addSourceFolder(String dir) throws Exception {
        DocUIStrings docUIStrings = new DocUIStrings();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create broadcast receiver that will tell when srcUri has been
        // really added to prefs.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.srcUris_Added":
                        srcAdded = true;
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.srcUris_Added");
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, filter);

        Log.d("ETATest", "before click on 'Add'");
        onView(withId(R.id.select_path_button)).perform(click());
        Log.d("ETATest", "after click on 'Add'");

        waitForDocumentsUiReadiness(device, docUIStrings);

        // Get the components of the path we want to add to the dir.
        String[] dirnames = dir.split(System.getProperty("file.separator"));

        /*
         * A. If it is already displayed, click on DCIM (aka parentDir), then the folders.
         * B. If DCIM is not there, try to display the root volume which holds DCIM.
         *    After what it is possible to click on DCIM & the subfolders.
         */

        // Search parentDir on the screen
        UiObject parentDir = device.findObject(new UiSelector().text(dirnames[0]).resourceId("android:id/title"));
        Log.d("ETATest", "parentDir (" + dirnames[0] + ") exists? " + parentDir.exists());

        // If parent dir is not there, we need to select the root volume in DocumentsUI
        if (!parentDir.exists()) {
            displayRoot();
        }

        // Now content of root volume should be displayed. So proceed with navigating
        // in the tree.
        selectDirFromRoot(dirnames);

        // Wait until we received the com.exifthumbnailadder.app.srcUris_Added message
        // or until timeout
        long max_duration = 1000;
        long timeout = System.currentTimeMillis() + max_duration;
        while (!srcAdded && System.currentTimeMillis() < timeout) {
            Thread.sleep(50);
        }

        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(receiver);
    }

    private static void clickObject(UiDevice device, UiObject object) throws UiObjectNotFoundException {
        object.clickAndWaitForNewWindow();
        device.waitForIdle();
    }

    private static boolean clickHamburgerMenuThenVolumeName(UiDevice device, UiObject hamburgerMenu, UiObject volumeName) throws UiObjectNotFoundException {
        clickObject(device, hamburgerMenu);

        // If volumeName is displayed, click on it.
        if (volumeName.exists()) {
            Log.d("ETATest", "volumeName exists. Click on volumeName.");
            clickObject(device, volumeName);
            return true;
        } else {
            // Get out of drawer/hamburger menu. Swipe on the drawer, because
            // clicking somewhere else doesn't seems a strategy with good results
            UiObject drawerRoots = device.findObject(new UiSelector().resourceId(new DocUIStrings().getDocumentsUiPackageName() + ":id/drawer_roots"));
            Log.d("ETATest", "drawerRoots exists? " + drawerRoots.exists());
            drawerRoots.swipeLeft(10);
            device.waitForIdle();
            return false;
        }
    }

    /* Display the root of the volume in the DocumentsUi
     * Layout of DocumentsUI differs across Android versions.
     * So method to get there is a bit complex
     */
    private static void displayRoot() throws UiObjectNotFoundException, Exception {
        String volumeNameInFilePicker = Build.MODEL;
        String sdCardNameInFilePicker = getSdCardNameInFilePicker();

        DocUIStrings docUIStrings = new DocUIStrings();

        // Not always available. May be hidden when there is no SDCard on some APIs
        UiObject hamburgerMenu = device.findObject(new UiSelector().clickable(true).focusable(true).description(docUIStrings.getShowRoots()));
        Log.d("ETATest", "hamburgerMenu exists? " + hamburgerMenu.exists());

        UiObject advancedMenu = device.findObject(new UiSelector().clickable(true).description(docUIStrings.getMoreOptions()));
        Log.d("ETATest", "advancedMenu exists? " + advancedMenu.exists());

        // Only exists on API < 30
        UiObject dropdown_breadcrumb = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/dropdown_breadcrumb"));
        Log.d("ETATest", "dropdown_breadcrumb exists? " + dropdown_breadcrumb.exists());

        // Only exists on API >= 30
        UiObject horizontal_breadcrumb = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/horizontal_breadcrumb"));
        Log.d("ETATest", "horizontal_breadcrumb exists? " + horizontal_breadcrumb.exists());

        UiObject volumeName = device.findObject(new UiSelector().text(volumeNameInFilePicker).resourceId("android:id/title"));
        Log.d("ETATest", "volumeName exists? " + volumeName.exists());

        // Displayed in the "more options" menu
        UiObject showInternalStorage = device.findObject(new UiSelector().text(docUIStrings.getShowInternalStorage()));
        Log.d("ETATest", "showInternalStorage exists? " + showInternalStorage.exists());

        // If volumeName is displayed, click on it.
        if (volumeName.exists()) {
            Log.d("ETATest", "volumeName exists. Click on volumeName.");
            clickObject(device, volumeName);
            return;
        }

        // Check if volume name is in Hamburger Menu,
        // In that case, click on it.
        if (hamburgerMenu.exists()) {
            Log.d("ETATest", "hamburgerMenu exists. Open menu.");
            if (clickHamburgerMenuThenVolumeName(device, hamburgerMenu, volumeName))
                return;
        }

        // Volume name is not in Hamburger Menu, so enable it from
        // Advanced menu
        if (advancedMenu.exists()) {
            Log.d("ETATest", "advancedMenu exists. Open menu.");
            clickObject(device, advancedMenu);

            // Wait until advancedMenu is really open
            Boolean waitResult = device.wait(Until.hasObject(By.res(docUIStrings.getDocumentsUiPackageName() + ":id/content")), 4000);
            if (waitResult.equals(Boolean.TRUE)) {
                // In the Advanced menu, click on "show internal storage" (if it is there)
                Log.d("ETATest", "showInternalStorage exists? " + showInternalStorage.exists());
                if (showInternalStorage.exists()) {
                    Log.d("ETATest", "showInternalStorage exists. Click on it.");
                    clickObject(device, showInternalStorage);

                    if (hamburgerMenu.exists()) {
                        Log.d("ETATest", "hamburgerMenu exists. Open menu.");
                        if (clickHamburgerMenuThenVolumeName(device, hamburgerMenu, volumeName))
                            return;
                    }
                } else {
                    Log.d("ETATest", "'Show internal storage' item not found");
                    device.pressBack();
                    Log.d("ETATest", "Doing 'pressBack()'");
                    device.waitForIdle();
                }
            } else {
                Log.e("ETATest", "advancedMenu didn't open before timeout");
            }
        }

        // Last resort: Breadcrumb. In some cases (when we can't open the drawer)
        // we may have to select the root volume by selecting it in the breadcrumb
        if (dropdown_breadcrumb.exists()) {
            Log.d("ETATest", "dropdown_breadcrumb exists. Click on it.");
            clickObject(device, dropdown_breadcrumb);

            if (volumeName.exists()) {
                Log.d("ETATest", "volumeName exists. Click on volumeName.");
                clickObject(device, volumeName);
                return;
            }
        }

        if (horizontal_breadcrumb.exists()) {
            Log.d("ETATest", "horizontal_breadcrumb exists. Swipe on it.");

            UiObject breadcrumbVolumeName = device.findObject(new UiSelector().text(volumeNameInFilePicker).resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/breadcrumb_text"));
            Log.d("ETATest", "breadcrumbVolumeName exists? " + breadcrumbVolumeName.exists());

            // Swipe until we find the breadcrumbVolumeName
            long max_duration = 10000; // 10 sec
            long timeout = System.currentTimeMillis() + max_duration;
            while (!breadcrumbVolumeName.exists() && System.currentTimeMillis() < timeout) {
                horizontal_breadcrumb.swipeRight(20);
                Thread.sleep(200);
                device.waitForIdle();
            }

            if (breadcrumbVolumeName.exists()) {
                Log.d("ETATest", "breadcrumbVolumeName exists. Click on volumeName.");
                clickObject(device, breadcrumbVolumeName);
                return;
            }
        }

        throw new UiObjectNotFoundException("Couldn't find method to display root volume in DocumentsUI");
    }

    private static void selectDirFromRoot(String[] dirnames) throws UiObjectNotFoundException {
        UiObject uiElement;
        DocUIStrings docUIStrings = new DocUIStrings();

        // Navigate to the requested dir
        for (String basename : dirnames) {
            Boolean waitResult = device.wait(Until.hasObject(By.text(basename)), 4000);
            if (waitResult.equals(Boolean.TRUE)) {
                uiElement = device.findObject(new UiSelector().text(basename).resourceId("android:id/title"));
                Log.d("ETATest", "dir '" + basename + "' exists? " + uiElement.exists());
                clickObject(device, uiElement);
            } else {
                Log.e("ETATest", basename + " dir not found.");
            }
        }

        // Confirm/Validate the selected dir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            Log.d("ETATest", "AllowAccessTo exists? " + uiElement.exists());
            clickObject(device, uiElement);

            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            Log.d("ETATest", "Allow exists? " + uiElement.exists());
            clickObject(device, uiElement);
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            Log.d("ETATest", "Select exists? " + uiElement.exists());
            clickObject(device, uiElement);
        }
    }

    public static void givePermissionToWorkingDir() throws Exception {
        UiObject uiElement;

        DocUIStrings docUIStrings = new DocUIStrings();

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        waitForDocumentsUiReadiness(device, docUIStrings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            clickObject(device, uiElement);
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            clickObject(device, uiElement);
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            clickObject(device, uiElement);
        } else {
            device.waitForIdle();
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            clickObject(device, uiElement);
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            clickObject(device, uiElement);
        }
    }

    public static void waitForDocumentsUiReadiness(UiDevice device, DocUIStrings docUIStrings) throws Exception {
        // DocumentsUI is very long to load the first time. ~10secs...
        device.wait(Until.hasObject(By.pkg(docUIStrings.getDocumentsUiPackageName())), 60000);
        Log.d("ETATest", "After wait until hasObject from documentsUi");

        // Additional wait because the screen refreshes itself even after the
        // first display (especially the first time DocumentsUI is loaded)
        // The next times, we might hit the timeout because the windows doesn't refresh itself.
        device.waitForWindowUpdate(docUIStrings.getDocumentsUiPackageName(), 5000);
        Log.d("ETATest", "after waitForWindowUpdate");

        // Wait until the device is idle (this is usually very short, maybe useless)
        device.waitForIdle();
        Log.d("ETATest", "after waitForIdle");
    }

    public void openHamburgerMenuBySwipe() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        DocUIStrings docUIStrings = new DocUIStrings();
        UiObject drawer = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/drawer_layout"));
        drawer.swipeRight(50);
        drawer.waitForExists(250);
    }

    public static void workingDirPermActivityCheckPermission() {
        ViewInteraction materialButton3 = onView(
                allOf(withId(R.id.button_checkPermissions), withText(R.string.working_dir_button_create_dir_and_set_permissions),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.permScrollView),
                                        0),
                                1)));
        materialButton3.perform(scrollTo(), click());
    }

    public static void openSettingsFragment() throws Exception {
        // The method below sometimes shows the Tooltip instead of opening the settings fragment (on API >= 31)
        onView(withId(R.id.SettingsFragment)).perform(click(click()));

        // So we use this one instead
//        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//        UiObject uiElement = device.findObject(new UiSelector().clickable(true).description(context.getString(R.string.action_settings)));
//        uiElement.clickAndWaitForNewWindow();
    }

    public static void openSyncFragment() throws Exception {
        onView(withId(R.id.SyncFragment)).perform(click(click()));
    }

    public static void clickPermissionAllowButton() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        String resource, permPackage;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permPackage = "com.android.packageinstaller";
        } else {
            permPackage = "com.android.permissioncontroller";
        }
        resource = permPackage + ":id/permission_allow_button";

        // Wait until permission controller is displayed
        device.waitForWindowUpdate(permPackage, 5000);
        UiObject uiElement = device.findObject(new UiSelector().clickable(true).resourceId(resource));
        uiElement.clickAndWaitForNewWindow();
    }

    public static void clickPermissionDenyButton() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        String resource, permPackage;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permPackage = "com.android.packageinstaller";
        } else {
            permPackage = "com.android.permissioncontroller";
        }
        resource = permPackage + ":id/permission_deny_button";

        // Wait until permission controller is displayed
        device.waitForWindowUpdate(permPackage, 5000);
        UiObject uiElement = device.findObject(new UiSelector().clickable(true).resourceId(resource));
        uiElement.clickAndWaitForNewWindow();
    }

    public static void clickPermissionDontAskAgainButton() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        String resource, permPackage;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permPackage = "com.android.packageinstaller";
        } else {
            permPackage = "com.android.permissioncontroller";
        }
        resource = permPackage + ":id/permission_deny_dont_ask_again_button";

        // Wait until permission controller is displayed
        device.waitForWindowUpdate(permPackage, 5000);
        UiObject uiElement = device.findObject(new UiSelector().clickable(true).resourceId(resource));
        uiElement.clickAndWaitForNewWindow();
    }

    public static void clearDocumentsUI() throws Exception {
        // This is needed so that when we enter DocumentsUI (aka file picker)
        // we always start with the same starting point in the app (not the last selected location)
        // It avoids test flakiness
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());
        DocUIStrings d = new DocUIStrings();
        uiDevice.executeShellCommand("pm clear " + d.documentsUiPackageName);
    }

    public static void clearETA() throws Exception {
        // This will clean ETA
        // ETA Preferences Cleanup is managed through orchestrator's clearPackageData: 'true'
        //TestUtil.clearETAPreferences();
        TestUtil.deleteWorkingDir();
    }

    public static void clearETAPreferences() {
        // This will clean ETA's preferences
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    public static void deleteWorkingDir() throws IOException {
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());
        uiDevice.executeShellCommand("rm -fr /storage/emulated/0/ThumbAdder");
        uiDevice.executeShellCommand("rm -fr /storage/emulated/0/JustSomething");
    }

    /**
     * Perform action of waiting for a specific view id.
     * @param viewId The id of the view to wait for.
     * @param millis The timeout of until when to wait for.
     */
    // https://stackoverflow.com/a/49814995/15401262
    public static ViewAction waitId(final int viewId, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific view with id <" + viewId + "> during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                final Matcher<View> viewMatcher = withId(viewId);

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
