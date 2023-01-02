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
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

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

        UiObject uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + context.getString(R.string.settings_button_add_dir)));
        uiElement.clickAndWaitForNewWindow();

        // Open "more options" menu to click on Show internal storage if it is there
        UiObject advancedMenu = device.findObject(new UiSelector().description(docUIStrings.getMoreOptions()));
        advancedMenu.clickAndWaitForNewWindow();

        // Click on "Show internal storage" if it is there, otherwise press back to quit the "More options" menu
        UiObject showInternalStorage = device.findObject(new UiSelector().text(docUIStrings.getShowInternalStorage()));
        try { showInternalStorage.clickAndWaitForNewWindow(); }
        catch (UiObjectNotFoundException e) { device.pressBack(); }

        // Open Drawer (aka Hamburger menu)
        UiObject hamburgerMenu = device.findObject(new UiSelector().descriptionMatches("(?i)" + docUIStrings.getShowRoots()));
        try {
            hamburgerMenu.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            // In some cases (when there is no sdcard for example), the hamburger menu doesn't exist.
            // So skip gracefully to the next step
            e.printStackTrace();
        }

        // Select Root (volume)
        //uiElement = device.findObject(new UiSelector().textMatches("(?i).*Virtual.*"));
        //uiElement = device.findObject(new UiSelector().textMatches("(?i)"+sdCardNameInFilePicker)); //DOESN'T WORK
        uiElement = device.findObject(new UiSelector().textMatches("(?i)" + volumeNameInFilePicker));
        uiElement.clickAndWaitForNewWindow();

        // Select folder
        String[] dirnames = dir.split(System.getProperty("file.separator"));
        for (String basename : dirnames) {
            uiElement = device.findObject(new UiSelector().textContains(basename));
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
}
