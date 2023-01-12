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
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import java.io.IOException;

public class TestUtil {

    public static void requestAllFilesAccess() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String permit_manage_external_storage = new String();
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
        String volumeNameInFilePicker = Build.MODEL;
        String sdCardNameInFilePicker = getSdCardNameInFilePicker();

        DocUIStrings docUIStrings = new DocUIStrings();

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        onView(withId(R.id.select_path_button)).perform(click());
//        UiObject uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + context.getString(R.string.settings_button_add_dir)));
//        uiElement.clickAndWaitForNewWindow();

        // Wait a little bit because sometimes, the next step (show more options menu)
        // doesn't seem to be clicked and the documentsUi seems refreshed a few times.
        //device.waitForWindowUpdate(docUIStrings.getDocumentsUiPackageName(), 2000);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Open "more options" menu to click on Show internal storage if it is there
            UiObject advancedMenu = device.findObject(new UiSelector().clickable(true).description(docUIStrings.getMoreOptions()));
            advancedMenu.clickAndWaitForNewWindow();

            // Click on "Show internal storage" if it is there, otherwise press back to quit the "More options" menu
            // Show internal storage is not needed anymore since Android R/30
            UiObject showInternalStorage = device.findObject(new UiSelector().text(docUIStrings.getShowInternalStorage()));
//        if (showInternalStorage.exists()) {
//            showInternalStorage.clickAndWaitForNewWindow();
//        } else {
//            Log.w("ETA", "'Show internal storage' item not found");
//            device.pressBack();
//        }
            try { showInternalStorage.clickAndWaitForNewWindow(); }
            catch (UiObjectNotFoundException e) {
                Log.w("ETA", "Show internal storage not found. Is the 'More options' menu open? Trying again...");
                advancedMenu.clickAndWaitForNewWindow();
                showInternalStorage = device.findObject(new UiSelector().text(docUIStrings.getShowInternalStorage()));
                showInternalStorage.clickAndWaitForNewWindow();
            }
        }
        // Open Drawer (aka Hamburger menu)
        UiObject hamburgerMenu = device.findObject(new UiSelector().clickable(true).description(docUIStrings.getShowRoots()));
        try {
            hamburgerMenu.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            // In some cases (when there is no sdcard for example), the hamburger menu doesn't exist.
            // So skip gracefully to the next step
            //e.printStackTrace();
            throw e;
        }

        // Select Root (volume)
        //uiElement = device.findObject(new UiSelector().textMatches("(?i).*Virtual.*"));
        //uiElement = device.findObject(new UiSelector().textMatches("(?i)"+sdCardNameInFilePicker)); //DOESN'T WORK
        UiObject uiElement = device.findObject(new UiSelector().text(volumeNameInFilePicker).resourceId("android:id/title"));
//        try {
            uiElement.clickAndWaitForNewWindow();
/*
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
            // In some cases (when we can't open the drawer), we may have to select the root by selecting it in the breadcrumb
            UiObject dropdown_breadcrumb = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/dropdown_breadcrumb"));
            if (dropdown_breadcrumb.exists()) {
                dropdown_breadcrumb.clickAndWaitForNewWindow();
                UiObject dropdownItem = device.findObject(new UiSelector().text(volumeNameInFilePicker).resourceId("android:id/title"));
                dropdownItem.clickAndWaitForNewWindow();
            } else {
                // dropdown_breadcrumb was removed in android 11, so try horizontal_breadcrumb
                // Swipe on horizontal_breadcrumb
                UiObject horizontal_breadcrumb = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/horizontal_breadcrumb"));
                UiObject root = null;
                for (int i = 0; i < 5; i++) {
                    horizontal_breadcrumb.swipeRight(20);
                    root = device.findObject(new UiSelector().text(volumeNameInFilePicker).resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/breadcrumb_text"));
                    if (root.exists())
                        break;
                }
                if (root != null)
                    root.clickAndWaitForNewWindow();
            }
        }
 */

        // Select folder
        String[] dirnames = dir.split(System.getProperty("file.separator"));
        for (String basename : dirnames) {
            uiElement = device.findObject(new UiSelector().text(basename).resourceId("android:id/title"));
            uiElement.clickAndWaitForNewWindow();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            uiElement.clickAndWaitForNewWindow();

            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            uiElement.clickAndWaitForNewWindow();
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            uiElement.clickAndWaitForNewWindow();
        }
    }

    public static void givePermissionToWorkingDir() throws Exception {
        UiObject uiElement;

        DocUIStrings docUIStrings = new DocUIStrings();

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            uiElement.clickAndWaitForNewWindow();
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            uiElement.clickAndWaitForNewWindow();
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            uiElement.clickAndWaitForNewWindow();
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            uiElement.clickAndWaitForNewWindow();
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            uiElement.clickAndWaitForNewWindow();
        }
    }

    public void openHamburgerMenuBySwipe() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        DocUIStrings docUIStrings = new DocUIStrings();
        UiObject drawer = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName() + ":id/drawer_layout"));
        drawer.swipeRight(50);
        drawer.waitForExists(250);
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

    public static void clickPermissionAllowButton() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        String resource;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            resource = "com.android.packageinstaller:id/permission_allow_button";
        } else {
            resource = "com.android.permissioncontroller:id/permission_allow_button";
        }
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

    public static void resetETAPermissions() throws IOException {
        // This will reset ETA's permissions
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());
        DocUIStrings d = new DocUIStrings();
        // This command makes the test suite crash.
        // uiDevice.executeShellCommand("pm reset-permissions com.exifthumbnailadder.app.debug");

    }

    public static void deleteWorkingDir() throws IOException {
        UiDevice uiDevice = UiDevice.getInstance(getInstrumentation());
        DocUIStrings d = new DocUIStrings();
        uiDevice.executeShellCommand("rm -fr /storage/emulated/0/ThumbAdder");
    }
}
