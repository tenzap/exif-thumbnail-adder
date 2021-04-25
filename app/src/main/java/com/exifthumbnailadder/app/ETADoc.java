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
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public abstract class ETADoc {
    final String THUMB_EXT = "";
    final String SUFFIX_TMP = "tmp";
    final String SUFFIX_BACKUP = "bak";
    final String SUFFIX_DEST = "dest";
    final String d = File.separator;    // "/"

    final HashMap<String, String> suffixes = new HashMap<String, String>();

    final String pref_workingDir;
    final boolean pref_writeTmpToCacheDir;
    final boolean pref_writeThumbnailedToOriginalFolder;

    final Context ctx;
    final String volumeName;
    final String volumeRootPath;
    final ETASrcDir root;
    final boolean withVolumeName;

    protected ETADoc(Context ctx, ETASrcDir root, String volumeName, String volumeRootPath, boolean withVolumeName) {
        this.ctx = ctx;
        this.root = root;
        this.volumeName = volumeName;
        this.volumeRootPath = volumeRootPath;

        this.withVolumeName = withVolumeName;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        pref_workingDir = prefs.getString("working_dir", "ThumbAdder");
        pref_writeTmpToCacheDir = prefs.getBoolean("writeTmpToCacheDir", true);
        pref_writeThumbnailedToOriginalFolder = prefs.getBoolean("writeThumbnailedToOriginalFolder", false);

        if (pref_writeThumbnailedToOriginalFolder) {
            suffixes.put(SUFFIX_DEST,"");
        } else {
            suffixes.put(SUFFIX_DEST,".new");
        }
        suffixes.put(SUFFIX_TMP,".tmp");
        suffixes.put(SUFFIX_BACKUP,".bak");
    }

    // Common to ETADocUri & ETADocFile
    public abstract String getMainDir();
    public abstract String getSubDir();
    public abstract boolean exists();
    public abstract boolean isJpeg();
    public abstract long length();
    public abstract String getName();
    public abstract Uri getUri();
    public abstract InputStream inputStream() throws Exception;
    public abstract void createDirForTmp();
    public abstract void createDirForBackup();
    public abstract void createDirForDest();
    public abstract Object getOutputInTmp();
    public abstract String getTmpFSPathWithFilename();
    public abstract String getFullFSPath();
    public abstract void writeInTmp(ByteArrayOutputStream newImgOs) throws Exception;
    public abstract Uri getOutputFileUri(Uri tmpUri, String filename);
    public abstract boolean isFile();
    public abstract boolean isDirectory();
    public abstract boolean delete();
    public abstract String getDPath();

    abstract String getBaseDir(String dirId);

    // ETADocUri only
    public abstract Uri getSrcUri(String srcDirMainDir, String srcDirTreeId) throws Exception;
    public abstract String getTreeId();
    public abstract Uri getTmpUri();
    public abstract Uri getBackupUri();
    public abstract Uri getDestUri();

    // ETADocFile only
    public abstract Path getSrcPath(String srcFSPath);
    public abstract Path getTmpPath();
    public abstract Path getBackupPath();
    public abstract Path getDestPath();
    public abstract Path toPath();
    public abstract String getWritablePath();

    // Static
    public static boolean isJpegFile(File file) {
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());
        return mimeType != null && mimeType.equals("image/jpeg");
    }


    public static Uri getSrcDocumentUriFor(Uri contentUri, String mainDir, String sourceFileTreeIdForGetSrcUri, boolean withVolumeName) throws Exception {
        String storagePath =  UriUtil.getDVolId(contentUri) + ":";   // "primary:"
        String uriAuthority = contentUri.getAuthority();

        String baseDir, fullDir;

        if (storagePath.endsWith(":")) {
            // This is a URI path (primary:DCIM....)
            baseDir = storagePath + mainDir;
        } else {
            // TODO
            throw new UnsupportedOperationException();
            //baseDir = storagePath + File.separator + mainDir;
        }

        if (withVolumeName) {
            fullDir = baseDir + File.separator + UriUtil.getDSub(UriUtil.getDSub(UriUtil.getDDSub(contentUri)));
        } else {
            fullDir = baseDir + File.separator + UriUtil.getDSub(UriUtil.getDDSub(contentUri));
        }

        // Remove trailing "/"
        fullDir = Paths.get(fullDir).toString();

        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uriAuthority, sourceFileTreeIdForGetSrcUri);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        return outUri;
    }

    String getFullDir(String baseDir, boolean withVolumeName) {
        if (withVolumeName) {
            return baseDir + d + volumeName + d + getSubDir();
        } else {
            return baseDir + d + getSubDir();
        }
    }
}
