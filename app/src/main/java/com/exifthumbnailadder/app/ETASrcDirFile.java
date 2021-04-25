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
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.TreeSet;

public class ETASrcDirFile extends ETASrcDir {

    File etaDocsRoot;

    public ETASrcDirFile(Context ctx, File etaDocsRoot) {
        this.ctx = ctx;
        this.etaDocsRoot = etaDocsRoot;
        this.excluded = getExcludedPath();
    }

    public String getFSPath() {
        return etaDocsRoot.getPath();
    }

    public String getFSPathWithoutRoot() {
        return etaDocsRoot.getPath().substring(getVolumeRootPath().length());
    }

    public Object getDocsRoot() {
        return etaDocsRoot;
    }

    public Object getDocsSet() {
        return filesToProcessList(etaDocsRoot, 0, new File(excluded)); // TreeSet<File>
    }

    class FileComparator implements Comparator<File> {
        @Override
        public int compare(File e1, File e2) {
            return e1.getPath().compareTo(e2.getPath());
        }
    }

    protected TreeSet<DocumentFile> docFilesToProcessList(DocumentFile df, int level, String excluded) {
        throw new UnsupportedOperationException();
    }

    protected TreeSet<File> filesToProcessList(File dir, int level, File excluded) {
        TreeSet<File> treeSet = new TreeSet<File>(new ETASrcDirFile.FileComparator());
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                if (!aFile.getPath().startsWith(excluded.getPath())) {
                    for (int i = 0; i < level; i++) {
                        //System.out.print("\t");
                    }
                    if (aFile.isDirectory()) {
                        //System.out.println("[" + aFile.getName() + "]");
                        treeSet.addAll(filesToProcessList(aFile, level + 1, excluded));
                    } else {
                        //System.out.println(aFile.getName());
                        treeSet.add(aFile);
                    }
                }
            }
        }
        return treeSet;
    }

    public String getVolumeName() {
        // Inspired from
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/base/core/java/android/os/storage/StorageVolume.java;drc=1639e6b8eeaac34d44b1f1cd0d50a5c051852a65;l=321
        String volumeName = "";

        StorageManager myStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume mySV = myStorageManager.getStorageVolume(etaDocsRoot);
        Class<?> storageVolumeClazz = null;

        if (mySV.isPrimary()) {
            volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY;
        } else {
            try {
                storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                Method getUuid = storageVolumeClazz.getMethod("getUuid");
                String mFsUuid = (String) getUuid.invoke(mySV);
                volumeName = normalizeUuid(mFsUuid);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return volumeName;
    }

    public String getExcludedPath() {
        return etaDocsRoot.getPath() + File.separator + getSecStorageDirName();
    }

    public String getVolumeRootPath() {
        // Get mounted path of the volume holding this folder
        String volumeRootPath = "";
        // get the path of the root for the volume on which the files/dir are located.
        // Ex: file/dir = /storage/1507-270B/DCIM.new/  --> volumeRootPath =  /storage/1507-270B
        // Ex: file/dir = /storage/emulated/0/DCIM      --> volumeRootPath =  /storage/emulated/0
        StorageManager myStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume mySV = myStorageManager.getStorageVolume(etaDocsRoot);

        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            volumeRootPath = (String) getPath.invoke(mySV);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (MainApplication.enableLog) Log.i(MainApplication.TAG, "volumeRootPath: " + volumeRootPath);
        return volumeRootPath;
    }

}
