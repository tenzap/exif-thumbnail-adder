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

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

// This class should be run last since we enable 'all files access' which I couldn't
// remove through instrumentation. So any other tests run after this class will
// have 'All files access' enabled which we don't want.

@RunWith(AndroidJUnit4.class)
public class zAddThumbs extends AddThumbsCommon {
    @Before
    public void checkAPI30() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
    }

    @Test
    public void addThumbsSettingsAllFilesAccessOn() throws Exception {
        // Only starting from Android 11 / API 30
        HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
        opts.put("all_files_access", new Boolean(true));
        addThumbs(opts);
    }

    @Test
    public void addThumbsSettingsAllFilesAccessOnUpdateInSourceOff() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("writeThumbnailedToOriginalFolder", false);
        e.apply();
        // Only starting from Android 11 / API 30
        HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
        opts.put("all_files_access", new Boolean(true));
        addThumbs(opts);
    }

    @Test
    public void addThumbsSettingsExiv2withoutSAF_AllFilesAccess() throws Exception {
        TestUtil.openSettingsFragment();
        TestUtil.requestAllFilesAccess();
        Assume.assumeTrue(Environment.isExternalStorageManager());

        HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
        opts.put("all_files_access", new Boolean(true));

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_exiv2");
        e.putBoolean("useSAF", false);
        e.apply();

        addThumbs(opts);
    }
}
