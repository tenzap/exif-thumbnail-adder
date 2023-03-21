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
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

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

            UiObject uiElement2 = device.findObject(new UiSelector().text(permit_manage_external_storage));
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
            displayRoot(parentDir);
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

        if (!srcAdded) {
            throw new Exception("com.exifthumbnailadder.app.srcUris_Added message not received. Source folder not added.");
        }

        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(receiver);
    }

    private static boolean clickObject(UiDevice device, UiObject object) throws UiObjectNotFoundException {
        boolean result = object.clickAndWaitForNewWindow();
        device.waitForIdle();
        return result;
    }

    private static boolean clickHamburgerMenuThenVolumeName(UiDevice device, UiObject hamburgerMenuOL, UiObject volumeName) throws UiObjectNotFoundException {
        DocUIStrings docUIStrings = new DocUIStrings();
        UiObject2 hamburgerMenu;
        boolean retValue;

        BySelector hamburgerMenuSelector = By.desc(docUIStrings.getShowRoots()).clickable(true).focusable(true);

        // First click on HamburgerMenu
        waitUntilHasObject(device, hamburgerMenuSelector, "HamburgerMenu");

        hamburgerMenu = device.findObject(hamburgerMenuSelector);
        Log.d("ETATest", "HamburgerMenu: After call to findObject.");

        if (hamburgerMenu != null) {
            Log.d("ETATest", "HamburgerMenu: Click on it.");

            // Wait for idle, sometimes there can be delay.
            Log.d("ETATest", "Before waitForIdle");
            device.waitForIdle();
            Log.d("ETATest", "After waitForIdle");

            Log.d("ETATest", "Before click");
            hamburgerMenu.click();
            Log.d("ETATest", "After click");

            Log.d("ETATest", "Before waitForIdle: ");
            device.waitForIdle();
            Log.d("ETATest", "After waitForIdle: ");
        } else {
            Log.e("ETATest", "HamburgerMenu: Couldn't find matching object.");
            throw new UiObjectNotFoundException("HamburgerMenu: Couldn't find matching object.");
        }

        // Now check if it opened
        BySelector drawerRootsSelector = By.res(docUIStrings.getDocumentsUiPackageName() + ":id/drawer_roots");
        waitUntilHasObject(device, drawerRootsSelector, "drawer_roots");

        // Now check if roots_list is displayed
        String resRootsList = docUIStrings.getDocumentsUiPackageName() + ":id/roots_list";
        BySelector rootsListSelector = By.res(resRootsList);
        waitUntilHasObject(device, rootsListSelector.hasChild(By.clazz("android.widget.LinearLayout")), ":id/roots_list");

        // If volumeName is displayed, click on it.
        if (volumeName.exists()) {
            Log.d("ETATest", "volumeName exists. Click on volumeName.");
            clickObject(device, volumeName);
            retValue = true;
        } else {
            // Get out of drawer/hamburger menu. Click on the 1st item in the drawer. Because:
            //  - Swipe on the drawer doesn't always respond correctly (esp on API29): too slow, nothing happens
            //  - clicking somewhere else doesn't seems a strategy with good results

            // Select the first child of ":id/roots_list"
            UiSelector uiSelector = new UiSelector().resourceId(resRootsList).childSelector(new UiSelector().className("android.widget.LinearLayout")).index(0);

            UiObject drawerItem = device.findObject(uiSelector);

            Log.d("ETATest", "drawerItem exists? " + drawerItem.exists());

            if (drawerItem.exists()) {
                Log.d("ETATest", "drawerItem exists. Open menu.");
                clickObject(device, drawerItem);
            }

            retValue = false;
        }

        // Wait until drawer_roots closed
        waitUntilGone(device, drawerRootsSelector, "drawer_roots");

        return retValue;
    }

    /* Display the root of the volume in the DocumentsUi
     * Layout of DocumentsUI differs across Android versions.
     * So method to get there is a bit complex
     */
    private static void displayRoot(UiObject parentDir) throws UiObjectNotFoundException, Exception {
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
            Boolean waitResult = null;

            // Use a BySelector by class, because, there can be id/title in the main DocumentsUI window
            //  Hierarchy is on API28/phone & API26/phone for main docUi windows:  LinearLayout[]/FrameLayout[]/LinearLayout[]/TextView["id/title"]
            //  Hierarchy is on API26/phone & API26/tablet7" for advancedMenu:  LinearLayout[]/RelativeLayout[]/TextView["id/title"]
            //  Hierarchy is on API28/phone for advancedMenu:  LinearLayout[]/LinearLayout["id/content"]/RelativeLayout[]/TextView["id/title"]
            //  Hierarchy is on API28/tablet7" for advancedMenu:  LinearLayout[]/LinearLayout["id/content"]/TextView[id:title]
            if (Build.VERSION.SDK_INT <= 27) {
                waitUntilHasObject(device,
                        By.clazz("android.widget.LinearLayout")
                                .hasChild(By.clazz("android.widget.RelativeLayout")
                                        .hasChild(By.clazz("android.widget.TextView").res("android:id/title")
                                        )
                                )
                        , "advancedMenu");
            } else if (Build.VERSION.SDK_INT == 28) {
                try {
                    waitUntilHasObject(device,
                            By.clazz("android.widget.LinearLayout")
                                    .hasChild(By.clazz("android.widget.LinearLayout").res("android:id/content")
                                            .hasChild(By.clazz("android.widget.RelativeLayout")
                                                    .hasChild(By.clazz("android.widget.TextView").res("android:id/title")
                                                    )
                                            )
                                    )
                            , "advancedMenu");
                } catch (UiObjectNotFoundException e) {
                    Log.e("ETATest", "object not found, checking the case for a tablet");
                    // On a tablet with API 28, there is a linearlayout, not a relativelayout
                    waitUntilHasObject(device,
                            By.clazz("android.widget.LinearLayout")
                                    .hasChild(By.clazz("android.widget.LinearLayout").res("android:id/content")
                                            .hasChild(By.clazz("android.widget.TextView").res("android:id/title")
                                            )
                                    )
                            , "advancedMenu");
                }
            } else {
                waitUntilHasObject(device, By.clazz("android.widget.LinearLayout").res(docUIStrings.getDocumentsUiPackageName() + ":id/content"), "advancedMenu");
            }

            // In the Advanced menu, click on "show internal storage" (if it is there)
            if (showInternalStorage.exists()) {
                Log.d("ETATest", "showInternalStorage exists. Click on it.");
                BySelector selector = By.text(docUIStrings.getShowInternalStorage());
                clickObject(device, selector, "showInternalStorage");

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

        // Last check, in case parentDir would exist... before throwing exception
        if(!parentDir.exists())
            throw new UiObjectNotFoundException("Couldn't find method to display root volume in DocumentsUI");
    }

    private static void selectDirFromRoot(String[] dirnames) throws UiObjectNotFoundException {
        UiObject uiElement;
        UiObject2 folder;
        DocUIStrings docUIStrings = new DocUIStrings();

        // Navigate to the requested dir
        for (String basename : dirnames) {
            BySelector folderSelector = By.text(basename);
            // On API >= 29 we could also use an additional filter ':id/item_root'
            //BySelector folderSelector = By.res(docUIStrings.getDocumentsUiPackageName() + ":id/item_root").hasDescendant(By.text(basename));
            waitUntilHasObject(device, folderSelector, "Folder '" + basename + "'");

            //folder = device.findObject(new UiSelector().text(basename).resourceId("android:id/title"));
            folder = device.findObject(folderSelector);
            Log.d("ETATest", "Folder '" + basename + "'. After call to findObject.");

            if (folder != null) {
                Log.d("ETATest", "Folder '" + basename + "' object found. Click on it.");

                // Wait for idle, sometimes there can be delay.
                Log.d("ETATest", "Before waitForIdle");
                device.waitForIdle();
                Log.d("ETATest", "After waitForIdle");

                Log.d("ETATest", "Before click");
                folder.click();
                Log.d("ETATest", "After click");

                Log.d("ETATest", "Before waitForIdle: ");
                device.waitForIdle();
                Log.d("ETATest", "After waitForIdle: ");

                // Wait until the folder is entered.
                if (Build.VERSION.SDK_INT <= 29) {
                    folderSelector = By.res(docUIStrings.getDocumentsUiPackageName() + ":id/dropdown_breadcrumb").hasDescendant(By.text(basename));
                    waitUntilHasObject(device, folderSelector, "Breadcrumb header text is '" + basename + "'");
                } else {
                    // Starting from API30, we can check the presence of "Files in <folder>" label
                    folderSelector = By.res(docUIStrings.getDocumentsUiPackageName() + ":id/header_title").text(Pattern.compile(docUIStrings.getFilesIn(basename), Pattern.CASE_INSENSITIVE));
                    waitUntilHasObject(device, folderSelector, "label 'Files in " + basename + "'");
                }
            } else {
                Log.e("ETATest", "Folder '" + basename + "'. Couldn't find matching object.");
                throw new UiObjectNotFoundException("Folder '" + basename + "'. Couldn't find matching object.");
            }
        }

        // Confirm/Validate the selected dir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String label = docUIStrings.getAllowAccessTo();
            BySelector selector = By.clickable(true).text(Pattern.compile(label.toUpperCase(), Pattern.CASE_INSENSITIVE));
            clickObject(device, selector, label);

            label = docUIStrings.getAllow();
            selector = By.clickable(true).text(Pattern.compile(label.toUpperCase(), Pattern.CASE_INSENSITIVE));
            clickObject(device, selector, label);
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

        Log.d("ETATest", "Before waitForDocumentsUiReadiness()");
        waitForDocumentsUiReadiness(device, docUIStrings);
        Log.d("ETATest", "After waitForDocumentsUiReadiness()");

        String label;
        BySelector selector;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            label = docUIStrings.getSave();
            selector = By.clickable(true).text(Pattern.compile(label, Pattern.CASE_INSENSITIVE)).enabled(true);
            clickObject(device, selector, label);

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            label = prefs.getString("working_dir", "ThumbAdder");
            selector = By.clickable(true).text(label);
            waitUntilGone(device, selector, label);

            label = docUIStrings.getAllowAccessTo();
            selector = By.clickable(true).text(Pattern.compile(label.toUpperCase(), Pattern.CASE_INSENSITIVE));
            clickObject(device, selector, label);

            label = docUIStrings.getAllow();
            selector = By.clickable(true).text(Pattern.compile(label.toUpperCase(), Pattern.CASE_INSENSITIVE));
            clickObject(device, selector, label);
        } else {
            label = docUIStrings.getSave();
            selector = By.clickable(true).text(Pattern.compile(label, Pattern.CASE_INSENSITIVE)).enabled(true);
            clickObject(device, selector, label);

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            label = prefs.getString("working_dir", "ThumbAdder");
            selector = By.clickable(true).text(label);
            waitUntilGone(device, selector, label);

            label = docUIStrings.getSelect();
            selector = By.clickable(true).text(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
            clickObject(device, selector, label);
        }
    }

    private static void clickObject(UiDevice device, BySelector selector, String label) throws UiObjectNotFoundException {
        waitUntilHasObject(device, selector, label);

        Log.d("ETATest", "'" + label + "' object: Before findObject");
        UiObject2 object = device.findObject(selector);

        if (object == null) {
            Log.e("ETATest", "'" + label + "' object: Couldn't find matching object.");
            throw new UiObjectNotFoundException("'" + label + "' object: Couldn't find matching object.");
        }

        Log.d("ETATest", "'" + label + "' object: Before click");
        object.click();

        waitUntilGone(device, selector, label);
    }

    private static void waitUntilHasObject(UiDevice device, BySelector selector, String label) throws UiObjectNotFoundException {
        waitUntilHasObject(device, selector, label, 10000);
    }

    private static void waitUntilHasObject(UiDevice device, BySelector selector, String label, int timeout) throws UiObjectNotFoundException {
        Log.d("ETATest",  label + ": Start waiting for hasObject.");
        Boolean waitResult = device.wait(Until.hasObject(selector), timeout);
        Log.d("ETATest", label + ": Finished waiting for hasObject.");

        if (!waitResult.equals(Boolean.TRUE)) {
            Log.e("ETATest", label + ": Not found before timeout.");
            throw new UiObjectNotFoundException(label + ": Not found before timeout.");
        }
        Log.d("ETATest", label + ": Now displayed on screen.");
    }

    private static void waitUntilGone(UiDevice device, BySelector selector, String label) throws UiObjectNotFoundException {
        Log.d("ETATest", "'" + label + "' object: Before wait gone");
        Boolean waitResult = device.wait(Until.gone(selector), 10000);
        if (!waitResult.equals(Boolean.TRUE)) {
            Log.e("ETATest", "'" + label + "' object: didn't get away from window");
            throw new UiObjectNotFoundException("'" + label + "' object: didn't get away from window before timeout");
        }
        Log.d("ETATest", "'" + label + "' object: Now gone");
    }

    public static void waitForDocumentsUiReadiness(UiDevice device, DocUIStrings docUIStrings) throws Exception {
        // DocumentsUI is very long to load the first time. ~10secs...
        waitUntilHasObject(device, By.pkg(docUIStrings.getDocumentsUiPackageName()), "documentsUi package", 60000);

        // Wait until the :id/dir_list pane is there because it can take a long time to display
        waitUntilHasObject(device, By.pkg(docUIStrings.getDocumentsUiPackageName()), ":id/dir_list", 60000);

        // Additional wait because the screen refreshes itself even after the
        // first display (especially the first time DocumentsUI is loaded)
        // The next times, we might hit the timeout because the windows doesn't refresh itself.
//        device.waitForWindowUpdate(docUIStrings.getDocumentsUiPackageName(), 5000);
//        Log.d("ETATest", "after waitForWindowUpdate");

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
        Log.d("ETATest", "Before scroll() & click() on WorkingDirPermActivity button");
        materialButton3.perform(scrollTo(), click());
        Log.d("ETATest", "After scroll() & click() on WorkingDirPermActivity button");

        device.wait(Until.gone(By.res(InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + ":string/working_dir_button_create_dir_and_set_permissions")), 10000);

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

    public static void clickPermissionButton(String action) throws Exception {
        String res_suffix;

        switch (action) {
            case "allow":
                res_suffix = ":id/permission_allow_button";
                break;
            case "deny":
                res_suffix = ":id/permission_deny_button";
                break;
            case "deny_dont_ask_again":
                res_suffix = ":id/permission_deny_dont_ask_again_button";
                break;
            default:
                throw new UnsupportedOperationException("action '" + action + "' not supported in clickPermissionButton().");
        }

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        String resource, permPackage;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permPackage = "com.android.packageinstaller";
        } else {
            permPackage = "com.android.permissioncontroller";
        }
        resource = permPackage + res_suffix;

        // Wait until permission controller is displayed
        BySelector buttonSelector = By.res(resource).clickable(true);
        waitUntilHasObject(device, buttonSelector, action + "Button");

        UiObject2 button = device.findObject(buttonSelector);
        Log.d("ETATest", action + "Button: After call to findObject.");

        if (button != null) {
            Log.d("ETATest", action + "Button: Click on it.");

            // Wait for idle, sometimes there can be delay.
            Log.d("ETATest", "Before waitForIdle");
            device.waitForIdle();
            Log.d("ETATest", "After waitForIdle");

            Log.d("ETATest", "Before click");
            button.click();
            Log.d("ETATest", "After click");

            waitUntilGone(device, buttonSelector, action + " Button");

            Log.d("ETATest", "Before waitForIdle: ");
            device.waitForIdle();
            Log.d("ETATest", "After waitForIdle: ");
        } else {
            Log.e("ETATest", action + "Button: Couldn't find matching object.");
            throw new UiObjectNotFoundException(action + "Button: Couldn't find matching object.");
        }
    }

    public static void clickPermissionAllowButton() throws Exception {
        clickPermissionButton("allow");
    }

    public static void clickPermissionDenyButton() throws Exception {
        clickPermissionButton("deny");
    }

    public static void clickPermissionDontAskAgainButton() throws Exception {
        clickPermissionButton("deny_dont_ask_again");
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
