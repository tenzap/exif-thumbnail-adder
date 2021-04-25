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
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Arrays;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public abstract class ETADoc {
    final String THUMB_EXT = "";

    PathUtil pathUtil = null;
    Context ctx;
    String volumeName;
    ETADocs root;
    boolean withVolumeName;

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

}
