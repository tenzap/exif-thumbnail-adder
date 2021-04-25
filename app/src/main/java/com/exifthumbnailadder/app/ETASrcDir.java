/*
 * Copyright (C) 2021 Fab Stz <fabstz-it@yahoo.fr>
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

import android.content.Context;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public abstract class ETASrcDir {

    Context ctx;
    String excluded;

    public abstract String getFSPath();
    public abstract String getFSPathWithoutRoot();
    public abstract Object getDocsRoot();
    public abstract Object getDocsSet();
    public abstract String getVolumeName();
    public abstract String getExcludedPath();
    public abstract String getVolumeRootPath();
    public abstract boolean isPermOk();

    protected abstract TreeSet<DocumentFile> docFilesToProcessList(DocumentFile df, int level, String excluded);
    protected abstract TreeSet<File> filesToProcessList(File dir, int level, File excluded);

    private void listDirectory(File dir, int level) {
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                for (int i = 0; i < level; i++) {
                    //System.out.print("\t");
                }
                if (aFile.isDirectory()) {
                    //System.out.println("[" + aFile.getName() + "]");
                    listDirectory(aFile, level + 1);
                } else {
                    //System.out.println(aFile.getName());
                }
            }
        }
    }

    /** {@hide} */
    public static @Nullable
    String normalizeUuid(@Nullable String fsUuid) {
        return fsUuid != null ? fsUuid.toLowerCase(Locale.US) : null;
    }

    public String getSecStorageDirName() {
            String excludedPrefix = PreferenceManager.getDefaultSharedPreferences(ctx).getString("excluded_sec_vol_prefix", ctx.getString(R.string.pref_excludedSecVolPrefix_defaultValue));
            return excludedPrefix + getSecStorageVolName();
    }

    public String getSecStorageVolName() {
            return getSecVolumeName(true);
    }

    public String getSecVolumeName(boolean normalize) {
        String volumeName = "";
        StorageManager myStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> mySVs = myStorageManager.getStorageVolumes();
        Class<?> storageVolumeClazz = null;

        for (StorageVolume mySV : mySVs) {
            try {
                if (! mySV.isPrimary()) {
                    storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                    Method getUuid = storageVolumeClazz.getMethod("getUuid");
                    String mFsUuid = (String) getUuid.invoke(mySV);
                    if (normalize)
                        volumeName = normalizeUuid(mFsUuid);
                    else
                        volumeName = mFsUuid;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return volumeName;
    }

}
