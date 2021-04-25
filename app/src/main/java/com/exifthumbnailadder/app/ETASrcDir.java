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

public class ETASrcDir {

    Context ctx;
    Object etaDocsRoot; // Accepted classes: Uri or File
    String excluded;

    public ETASrcDir(Context ctx, Object etaDocsRoot) {
        if ( ! (etaDocsRoot instanceof Uri || etaDocsRoot instanceof File))
            throw new UnsupportedOperationException("etaDocsRoot should be of Class File or Uri");

        this.ctx = ctx;
        this.etaDocsRoot = etaDocsRoot;
        this.excluded = getExcludedPath();
    }

    public String getFSPath() {
        if (etaDocsRoot instanceof Uri) {
            return FileUtil.getFullPathFromTreeUri((Uri)etaDocsRoot, ctx);
        }
        if (etaDocsRoot instanceof File) {
            return ((File) etaDocsRoot).getPath();
        }
        return null;
    }

    public String getFSPathWithoutRoot() {
        if (etaDocsRoot instanceof Uri) {
            return UriUtil.getDPath((Uri)etaDocsRoot);
        }
        if (etaDocsRoot instanceof File) {
            return ((File) etaDocsRoot).getPath().substring(getVolumeRootPath().length());
        }
        return null;
    }

    public Object getDocsRoot() {
        return etaDocsRoot;
    }

    public Object getDocsSet() {
        if (etaDocsRoot instanceof Uri) {
            DocumentFile baseDf = DocumentFile.fromTreeUri(ctx, (Uri) etaDocsRoot);
            return docFilesToProcessList(baseDf, 0, excluded); // TreeSet<DocumentFile>
        }
        if (etaDocsRoot instanceof File) {
            return filesToProcessList((File)etaDocsRoot, 0, new File(excluded)); // TreeSet<File>
        }
        return null;
    }

    class DocumentFileComparator implements Comparator<DocumentFile> {
        @Override
        public int compare(DocumentFile e1, DocumentFile e2) {
            return e1.getUri().toString().compareTo(e2.getUri().toString());
        }
    }

    class FileComparator implements Comparator<File> {
        @Override
        public int compare(File e1, File e2) {
            return e1.getPath().compareTo(e2.getPath());
        }
    }

    private TreeSet<DocumentFile> docFilesToProcessList(DocumentFile df, int level, String excluded) {
        TreeSet<DocumentFile> treeSet = new TreeSet<DocumentFile>(new DocumentFileComparator());
        DocumentFile[] firstLevelFiles = df.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (DocumentFile aFile : firstLevelFiles) {
                for (int i = 0; i < level; i++) {
                    //System.out.print("\t");
                }
                if (aFile.isDirectory()) {
                    if (aFile.getName().equals(excluded)) {
                        if (MainApplication.enableLog) Log.i(MainApplication.TAG, ctx.getString(R.string.frag1_log_skipping_excluded_dir, excluded, aFile.getUri().getPath()));
                    } else {
                        //System.out.println("[" + aFile.getName() + "]");
                        treeSet.addAll(docFilesToProcessList(aFile, level + 1, excluded));
                    }
                } else {
                    //System.out.println(aFile.getName());
                    treeSet.add(aFile);
                }
            }
        }
        return treeSet;
    }

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

    private TreeSet<File> filesToProcessList(File dir, int level, File excluded) {
        TreeSet<File> treeSet = new TreeSet<File>(new FileComparator());
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
        if (etaDocsRoot instanceof File) {
            // Inspired from
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/base/core/java/android/os/storage/StorageVolume.java;drc=1639e6b8eeaac34d44b1f1cd0d50a5c051852a65;l=321
            String volumeName = "";

            StorageManager myStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume mySV = myStorageManager.getStorageVolume((File) etaDocsRoot);
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
        } else if (etaDocsRoot instanceof Uri) {
            return UriUtil.getTVolId((Uri)etaDocsRoot);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@hide} */
    public static @Nullable
    String normalizeUuid(@Nullable String fsUuid) {
        return fsUuid != null ? fsUuid.toLowerCase(Locale.US) : null;
    }

    public String getExcludedPath() {
        if (etaDocsRoot instanceof File) {
            return ((File) etaDocsRoot).getPath() + File.separator + getSecStorageDirName();
        } else if (etaDocsRoot instanceof Uri) {
            return getSecStorageDirName();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getSecStorageDirName() {
        if (etaDocsRoot instanceof File || etaDocsRoot instanceof Uri) {
            String excludedPrefix = PreferenceManager.getDefaultSharedPreferences(ctx).getString("excluded_sec_vol_prefix", ctx.getString(R.string.pref_excludedSecVolPrefix_defaultValue));
            return excludedPrefix + getSecStorageVolName();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getSecStorageVolName() {
        if (etaDocsRoot instanceof File || etaDocsRoot instanceof Uri) {
            return getSecVolumeName(true);
        } else {
            throw new UnsupportedOperationException();
        }
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

    public String getVolumeRootPath() {
        // Get mounted path of the volume holding this folder

        if (etaDocsRoot instanceof File) {
            String volumeRootPath = "";
            // get the path of the root for the volume on which the files/dir are located.
            // Ex: file/dir = /storage/1507-270B/DCIM.new/  --> volumeRootPath =  /storage/1507-270B
            // Ex: file/dir = /storage/emulated/0/DCIM      --> volumeRootPath =  /storage/emulated/0
            StorageManager myStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume mySV = myStorageManager.getStorageVolume((File)etaDocsRoot);

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
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
