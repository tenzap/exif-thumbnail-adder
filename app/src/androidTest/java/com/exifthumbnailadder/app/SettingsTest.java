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

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsTest {
    Context context;
    SharedPreferences prefs;

    @Rule
    public ActivityScenarioRule <MainActivity> activityScenarioRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Test
    public void addFolder_test_pics() throws Exception {
        // Go to Settings
        onView(withId(R.id.SettingsFragment)).perform(click());

        // Add Folder in settings
        TestUtil.addSourceFolder("DCIM/test_pics");

        // Check that folder is in the list
        SharedPreferences.Editor editor = prefs.edit();
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));

        assertEquals(1, inputDirs.size());
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3ADCIM%2Ftest_pics/document/primary%3ADCIM%2Ftest_pics", inputDirs.get(0).toString());
    }
}
