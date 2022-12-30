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
import androidx.test.uiautomator.UiSelector;

public class TestUtil {

    public static void requestAllFilesAccess() {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String permit_manage_external_storage = new String();
        int resId;

        PackageManager manager = context.getPackageManager();

        try {
            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/res/values/strings.xml
            Resources resources = manager.getResourcesForApplication("com.android.settings");
            resId = resources.getIdentifier("permit_manage_external_storage", "string", "com.android.settings");
            permit_manage_external_storage = resources.getString(resId);
        } catch (Exception e) { e.printStackTrace(); }

        onView(withText(R.string.pref_allFilesAccess_title)).perform(click());

        UiObject uiElement2 = device.findObject(new UiSelector().textMatches("(?i)" + permit_manage_external_storage));
        try { uiElement2.click(); }
        catch (Exception e) { e.printStackTrace(); }
        device.pressBack();
    }

    public static String getSdCardNameInFilePicker() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager manager = context.getPackageManager();
        int resId = 0;
        Resources resources = null;
        try {
            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/res/values/strings.xml
            resources = manager.getResourcesForApplication("com.android.settings");
            resId = resources.getIdentifier("sdcard_setting", "string", "com.android.settings");
        } catch (Exception e) { e.printStackTrace(); }
        return resources.getString(resId);
    }

    public static void addSourceFolder() {
        DocUIStrings docUIStrings = new DocUIStrings();

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        UiObject uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)"+context.getString(R.string.settings_button_add_dir)));
        try { uiElement.clickAndWaitForNewWindow(); }
        catch (Exception e) { e.printStackTrace(); }

        String volumeNameInFilePicker = Build.MODEL;
        String sdCardNameInFilePicker = getSdCardNameInFilePicker();

        //int iterations_count = (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) ? 2 : 1;
        int iterations_count = 1;
        for (int j=0; j<iterations_count; j++) {
            // Need to do it twice to be sure to catch the sd card. Sometimes it fails to do so.
            UiObject drawer = device.findObject(new UiSelector().resourceId(docUIStrings.getDocumentsUiPackageName()+":id/drawer_layout"));
            try {
                drawer.swipeRight(50);
                drawer.waitForExists(250);
            } catch (Exception e) { e.printStackTrace(); }

            //uiElement = device.findObject(new UiSelector().textMatches("(?i).*Virtual.*"));
            //uiElement = device.findObject(new UiSelector().textMatches("(?i)"+sdCardNameInFilePicker)); //DOESN'T WORK
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
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }

            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void givePermissionToWorkingDir() {
        UiObject uiElement;

        DocUIStrings docUIStrings = new DocUIStrings();

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textContains(docUIStrings.getAllowAccessTo()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getAllow()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        } else {
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSave()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
            uiElement = device.findObject(new UiSelector().clickable(true).textMatches("(?i)" + docUIStrings.getSelect()));
            try { uiElement.clickAndWaitForNewWindow(); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }
}
