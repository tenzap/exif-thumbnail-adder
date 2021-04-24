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
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Arrays;

public class ETADoc {

    DocumentFile docFile = null;
    Uri uri = null;
    File file = null;
    String type;
    PathUtil pathUtil = null;
    Context ctx;
    String volumeRootPath;
    String volumeName;
    ETADocs root;

    public ETADoc(DocumentFile docFile, Context ctx, ETADocs root) {
        type = "Uri";
        this.docFile = docFile;
        this.uri = docFile.getUri();
        this.ctx = ctx;
        this.volumeName = root.getVolumeName();
        this.root = root;

        pathUtil = new PathUtil(
                uri,
                getMainDir(),
                getSubDir(),
                this.volumeName,
                root.getSecStorageDirName(),
                PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    public ETADoc(File file, Context ctx, ETADocs root) {
        this.file = file;
        type = "File";
        this.ctx = ctx;
        this.volumeRootPath = root.getVolumeRootPath();
        this.volumeName = root.getVolumeName();
        this.root = root;

        pathUtil = new PathUtil(
                getWritablePath(),
                getMainDir(),
                getSubDir(),
                this.volumeName,
                root.getSecStorageDirName(),
                PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    public String getMainDir() {
        switch (type) {
            case "File":
                String mainDir = "";
                if (file.toString().startsWith(volumeRootPath)) {
                    String tmp = file.toString().substring(volumeRootPath.length()); // "/DCIM/dir1/s2/file.jpg"
                    String[] b = tmp.split(File.separator); // [0]: "" ; [1]: "DCIM; [2]: "dir1"...
                    mainDir = b[1]; // "DCIM"
                }
                return mainDir;
            case "Uri":
                return UriUtil.getDD1(uri);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getSubDir() {
        switch (type) {
            case "File":
                String subDir = "";
                if (file.toString().startsWith(volumeRootPath)) {
                    String tmp = file.toString().substring(volumeRootPath.length()); // "/DCIM/dir1/s2/file.jpg"
                    String[] b = tmp.split(File.separator); // [0]: "" ; [1]: "DCIM; [2]: "dir1"...
                    subDir = String.join(File.separator, Arrays.copyOfRange(b, 2, b.length - 1)); // "dir1/s2"
                }
                return subDir;
            case "Uri":
                return UriUtil.getDDSubParent(uri);
            default:
                throw new UnsupportedOperationException();

        }
    }

    public String getTreeId() {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return UriUtil.getTreeId(uri);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getDocIdParent() {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return UriUtil.getDParent(uri);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getSubPath() {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                String subPath = getDocIdParent().replace(getTreeId(), "");
                subPath = subPath.startsWith("/") ? subPath.replaceFirst("/", "") : subPath;
                return subPath;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public boolean exists() {
        switch (type) {
            case "File":
                return file.exists();
            case "Uri":
                return docFile.exists();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public boolean isJpeg() {
        switch (type) {
            case "File":
                return isJpegFile(file);
            case "Uri":
                return docFile.getType().equals("image/jpeg");
            default:
                throw new UnsupportedOperationException();
        }
    }

    public long length() {
        switch (type) {
            case "File":
                return file.length();
            case "Uri":
                return docFile.length();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getName() {
        switch (type) {
            case "File":
                return file.getName();
            case "Uri":
                return docFile.getName();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Uri getTmpUri(boolean withVolumeName) {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return pathUtil.getTmpUri(ctx, withVolumeName);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Uri getBackupUri(boolean withVolumeName) {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return pathUtil.getBackupUri(ctx, withVolumeName);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Uri getDestUri() {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return pathUtil.getDestUri(ctx);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getTmpDir(boolean withVolumeName) {
        switch (type) {
            case "File":
                return pathUtil.getTmpDir(ctx, withVolumeName);
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getBackupDir(boolean withVolumeName) {
        switch (type) {
            case "File":
                return pathUtil.getBackupDir(withVolumeName);
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getDestDir() {
        switch (type) {
            case "File":
                return pathUtil.getDestDir();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Uri getUri() {
        switch (type) {
            case "File":
                return Uri.fromFile(file);
            case "Uri":
                return uri;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public DocumentFile getDocumentFile() {
        return docFile;
    }

    public File getFile() {
        return file;
    }

    public FileInputStream fileInputStream() throws Exception {
        switch (type) {
            case "File":
                try {
                    return new FileInputStream(file);
                } catch (Exception e) {
                    throw e;
                }
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getPath() {
        switch (type) {
            case "File":
                return file.getPath();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Path toPath() {
        switch (type) {
            case "File":
                return file.toPath();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String toString() {
        switch (type) {
            case "File":
                return file.toString();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static boolean isJpegFile(File file) {
        String mimeType = URLConnection.guessContentTypeFromName(file.getPath());
        return mimeType != null && mimeType.equals("image/jpeg");
    }

    public String getWritablePath() {
        if (type.equals("File")) {
            // We can only write to primary external, so set writablePath accordingly
            String writablePath;
            if (volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                writablePath = volumeRootPath;
            } else {
                // Attention "getExternalStorageDirectory" deprecated from API 29,
                writablePath = Environment.getExternalStorageDirectory().toString();
            }
            return writablePath;
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
