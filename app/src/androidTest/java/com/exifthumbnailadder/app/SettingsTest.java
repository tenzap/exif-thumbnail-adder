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
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class SettingsTest extends TestCommons {
    Context context;
    SharedPreferences prefs;

    @Rule
    public ActivityScenarioRule <MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.executeShellCommand("mkdir -p /storage/emulated/0/DCIM/test_pics");
        TestUtil.clearETA();
        TestUtil.clearDocumentsUI();
    }

    @Test
    public void addFolder_test_pics() throws Exception {
        // Go to Settings
        TestUtil.openSettingsFragment();

        // Add Folder in settings
        TestUtil.addSourceFolder("DCIM/test_pics");

        // Check that folder is in the list
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        assertEquals("Expected only 1 dir", 1, inputDirs.size());
        assertEquals("Selected dir is not correct", "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2Ftest_pics/document/primary%3ADCIM%2Ftest_pics", inputDirs.get(0).toString());
    }

    @Test
    public void removeFolder() throws Exception {
        // Go to Settings
        TestUtil.openSettingsFragment();

        // Remove folders
        onView(withId(R.id.del_path_button)).perform(click());

        // Get preference value
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        // Check that folder list is empty
        assertEquals("Selected dirs is not empty", 0, inputDirs.size());
    }

    @Test
    public void addThenRemoveFolder() throws Exception {
        // Go to Settings
        TestUtil.openSettingsFragment();

        // Add Folder in settings
        TestUtil.addSourceFolder("DCIM/test_pics");

        // Get preference value
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        // Check that folder is in the list
        assertEquals("Expected only 1 dir",1, inputDirs.size());

        // Remove folders
        onView(withId(R.id.del_path_button)).perform(click());

        // Get preference value
        inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        // Check that folder list is empty
        assertEquals("Selected dirs is not empty",0, inputDirs.size());
    }

}
