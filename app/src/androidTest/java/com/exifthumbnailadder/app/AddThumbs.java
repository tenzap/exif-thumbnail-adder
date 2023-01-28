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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class AddThumbs extends AddThumbsCommon {

    @Test
    public void addThumbsSettingsDefault() throws Exception {
        addThumbs();
    }

    @Test
    public void addThumbsSettingsDefaultTwoRuns() throws Exception {
        HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
        opts.put("rerun_processing", new Boolean(true));
        addThumbs();
    }

    @Test
    public void addThumbsAltWorkDir() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putString("working_dir", "JustSomething");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsRotateOff() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("rotateThumbnails", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsSkipOff() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsFixMissingOff() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("fixHavingThumbnailButMissingTags", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsCreateBackupOff() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("backupOriginalPic", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsUpdateInSourceOffWithoutDestOverwrite() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("writeThumbnailedToOriginalFolder", false);
        e.apply();
        HashMap<String, Boolean> opts = new HashMap<String, Boolean>();
        opts.put("rerun_processing", new Boolean(true));
        addThumbs(opts);
    }

    @Test
    public void addThumbsSettingsUpdateInSourceOffWithDestOverwrite() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("writeThumbnailedToOriginalFolder", false);
        e.putBoolean("overwriteDestPic", true);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsExiv2() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_exiv2");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsExiv2SkipOnLogLevelError() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_exiv2");
        e.putString("exiv2SkipOnLogLevel", "error");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsExiv2SkipOnLogLevelNone() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_exiv2");
        e.putString("exiv2SkipOnLogLevel", "none");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsExiv2withoutSAF() throws Exception {
        // Skip test if we don't have "all files access"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Assume.assumeTrue(!Environment.isExternalStorageManager());
        }
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_exiv2");
        e.putBoolean("useSAF", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsLibExif() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_libexif");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsLibExifSkipOnErrorOFF() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_libexif");
        e.putBoolean("libexifSkipOnError", false);
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsAEE() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_android-exif-extended");
        e.apply();
        addThumbs();
    }

    @Test
    public void addThumbsSettingsPixymeta() throws Exception {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("skipPicsHavingThumbnail", false);
        e.putString("exif_library", "exiflib_pixymeta");
        e.apply();
        addThumbs();
    }

    @Test
    public void syncListTest() throws Exception {
        // Don't write to original folder so that we can test
        // that files DCIM.new are also 'synced"
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("writeThumbnailedToOriginalFolder", false);
        e.apply();

        addThumbs();

        deletePicture("Reconyx_HC500_Hyperfire.jpg");
        deletePicture("mobile/jolla.jpg");
        deletePicture("orientation/portrait_1.jpg");
        deletePicture("orientation/portrait_2.jpg");
        deletePicture("orientation/portrait_3.jpg");
        deletePicture("orientation/portrait_4.jpg");
        deletePicture("orientation/portrait_5.jpg");
        deletePicture("orientation/portrait_6.jpg");
        deletePicture("orientation/portrait_7.jpg");
        deletePicture("orientation/portrait_8.jpg");
        deletePicture("tests/87_OSError.jpg");

        syncList();
    }

    @Test
    public void syncDeleteTest() throws Exception {
        // Don't write to original folder so that we can test
        // that files DCIM.new are also 'synced"
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("writeThumbnailedToOriginalFolder", false);
        e.apply();

        addThumbs();

        deletePicture("Reconyx_HC500_Hyperfire.jpg");
        deletePicture("mobile/jolla.jpg");
        deletePicture("orientation/portrait_1.jpg");
        deletePicture("orientation/portrait_2.jpg");
        deletePicture("orientation/portrait_3.jpg");
        deletePicture("orientation/portrait_4.jpg");
        deletePicture("orientation/portrait_5.jpg");
        deletePicture("orientation/portrait_6.jpg");
        deletePicture("orientation/portrait_7.jpg");
        deletePicture("orientation/portrait_8.jpg");
        deletePicture("tests/87_OSError.jpg");

        syncDelete();
    }
}
