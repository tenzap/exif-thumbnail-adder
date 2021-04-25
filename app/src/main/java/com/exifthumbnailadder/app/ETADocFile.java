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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

public class ETADocFile extends ETADoc {

    final File etaDoc;

    public ETADocFile(File file, Context ctx, ETASrcDirFile root, boolean withVolumeName) {
        super(ctx, root, root.getVolumeName(), root.getVolumeRootPath(), withVolumeName);
        this.etaDoc = file;
    }

    public String getMainDir() {
        String mainDir = "";
        if (etaDoc.toString().startsWith(volumeRootPath)) {
            String tmp = etaDoc.toString().substring(volumeRootPath.length()); // "/DCIM/dir1/s2/file.jpg"
            String[] b = tmp.split(File.separator); // [0]: "" ; [1]: "DCIM; [2]: "dir1"...
            mainDir = b[1]; // "DCIM"
        }
        return mainDir;
    }

    public String getSubDir() {
        String subDir = "";
        if (etaDoc.toString().startsWith(volumeRootPath)) {
            String tmp = etaDoc.toString().substring(volumeRootPath.length()); // "/DCIM/dir1/s2/file.jpg"
            String[] b = tmp.split(File.separator); // [0]: "" ; [1]: "DCIM; [2]: "dir1"...
            if (b.length > 2)
                subDir = String.join(File.separator, Arrays.copyOfRange(b, 2, b.length - 1)); // "dir1/s2"
        }
        return subDir;
    }

    public String getTreeId() {
        throw new UnsupportedOperationException();
    }

    public boolean exists() {
        return etaDoc.exists();
    }

    public boolean isJpeg() {
        return isJpegFile(etaDoc);
    }

    public long length() {
        return etaDoc.length();
    }

    public String getName() {
        return etaDoc.getName();
    }

    public Uri getTmpUri() {
        throw new UnsupportedOperationException();
    }

    public Uri getBackupUri() {
        throw new UnsupportedOperationException();
    }

    public Uri getDestUri() {
        throw new UnsupportedOperationException();
    }

    protected String getTmpDir() {
        String dirId = SUFFIX_TMP;
        String baseDir, fullDir;

        if (pref_writeTmpToCacheDir) {
            baseDir = ctx.getExternalCacheDir() + d + getMainDir() + suffixes.get(dirId);
        } else {
            baseDir = getBaseDir(dirId);
        }

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        createNomediaFile(baseDir, getWritablePath() + d + getMainDir());

        if (enableLog) Log.i(TAG, "Writing files for '" + dirId + "' to: " + fullDir);
        return fullDir;
    }

    public Path getTmpPath() {
        return new File(getTmpDir()).toPath();
    }

    protected String getBackupDir() {
        String dirId = SUFFIX_BACKUP;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        createNomediaFile(baseDir, getWritablePath() + d + getMainDir());

        if (enableLog) Log.i(TAG, "Writing files for '" + dirId + "' to: " + fullDir);
        return fullDir;
    }

    public Path getBackupPath() {
        return new File(getBackupDir()).toPath();
    }

    protected String getDestDir() {
        String dirId = SUFFIX_DEST;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        if (volumeName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY)) {
            fullDir = getFullDir(baseDir, false);
        } else {
            fullDir = baseDir + d + root.getSecStorageDirName() + d + getSubDir();
        }

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        if (! suffixes.get(dirId).isEmpty())
            createNomediaFile(baseDir, getWritablePath() + d + getMainDir());
        return fullDir;
    }

    public Path getDestPath() {
        return new File(getDestDir()).toPath();
    }

    public Uri getUri() {
        return Uri.fromFile(etaDoc);
    }

    public InputStream inputStream() throws Exception {
        try {
            return new FileInputStream(etaDoc);
        } catch (Exception e) {
            throw e;
        }
    }

    public Path toPath() {
        return etaDoc.toPath();
    }

    public String getWritablePath() {
        // We can only write to primary external, so set writablePath accordingly
        String writablePath;
        if (volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
            writablePath = volumeRootPath;
        } else {
            // Attention "getExternalStorageDirectory" deprecated from API 29,
            writablePath = Environment.getExternalStorageDirectory().toString();
        }
        return writablePath;
    }

    public void createDirForTmp() {
        createDirFor(getTmpDir());
    }

    public void createDirForBackup() {
        createDirFor(getBackupDir());
    }

    public void createDirForDest() {
        createDirFor(getDestDir());
    }

    public Object getOutputInTmp() {
        Path outputFilename = getTmpPath().resolve(getName() + THUMB_EXT);
        return outputFilename.toFile(); // File
    }

    public String getTmpFSPathWithFilename() {
        return ((File)getOutputInTmp()).getPath();
    }

    public String getFullFSPath() {
        return etaDoc.getPath();
    }

    public void writeInTmp(ByteArrayOutputStream newImgOs) throws Exception {
        File outputFilename = (File) getOutputInTmp();
        if (enableLog) Log.i(TAG, "Write to: " + outputFilename);
        FileOutputStream outputStream;
        outputStream = new FileOutputStream(outputFilename);
        outputStream.write(newImgOs.toByteArray());
        outputStream.close();
        if (enableLog) Log.i(TAG, "Write to DONE: " + outputFilename);
    }

    public Uri getOutputFileUri(Uri tmpUri, String filename) {
        if (tmpUri.getScheme().equals("file")) {
            return Uri.withAppendedPath(tmpUri, filename);
        }

        Uri outputFileUri = null;
        DocumentFile outputFileDf = DocumentFile.fromTreeUri(ctx, tmpUri).findFile(filename);
        try {
            if (outputFileDf != null) {
                outputFileUri = outputFileDf.getUri();
            } else {
                outputFileUri = DocumentsContract.createDocument(
                        ctx.getContentResolver(),
                        tmpUri,
                        "image/jpg",
                        filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputFileUri;
    }

    public boolean isFile() {
        return etaDoc.isFile();
    }

    public boolean isDirectory() {
        return etaDoc.isDirectory();
    }

    public boolean delete() {
        return etaDoc.delete();
    }

    public String getDPath() {
        return etaDoc.toString().substring(volumeRootPath.length());
    }

    public Path getSrcPath(String srcFSPath) {
        String tmp = etaDoc.getPath(); // "/storage/emulated/0/ThumbAdder/DCIM.bak/external_primary/dir1/s2/image.jpg

        tmp = tmp.substring(volumeRootPath.length()); // "/ThumbAdder/DCIM.bak/external_primary/dir1/s2/image.jpg

        if (withVolumeName) {
            // remove the volumeName from the path
            tmp = tmp.replace(File.separator + volumeName + File.separator, File.separator); // "/ThumbAdder/DCIM.bak/dir1/s2/image.jpg"
        }

        tmp = UriUtil.getDSub(UriUtil.getDSub(UriUtil.getDSub(tmp))); // "dir1/s2/image.jpg"

        return new File(srcFSPath).toPath().resolve(tmp);
    }

    public Uri getSrcUri(String srcDirMainDir, String srcDirTreeId) throws Exception {

        throw new UnsupportedOperationException();
    }

    public static void createNomediaFile(String path, String exceptDir) {
        File thePath = new File(path);
        File theExceptDir = new File(exceptDir);

        if (theExceptDir.exists() && ! theExceptDir.isDirectory())
            return;

        // .../DCIM == .../DCIM
        if (thePath.equals(theExceptDir))
            return;

        String thePathString = thePath.toString();
        String theExceptDirString = theExceptDir.toString();
        // .../DCIM/a   &   .../DCIM/
        if ( thePathString.startsWith( theExceptDirString + File.separator ))
            return;

        // Don't create for folders at lower level than theExceptDirString
        if ( thePathString.startsWith( theExceptDirString ) && thePathString.split(File.separator).length >  theExceptDirString.split(File.separator).length)
            return;

        // We create folder if it doesn't exist
        if (!thePath.exists()) {
            // Dir doesn't exist. Creating it.
            thePath.mkdirs();
        }

        // Create nomedia file
        File nomediaFile = new File(path + "/.nomedia");
        try {
            nomediaFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createDirFor(String path) {
        File f = new File(path);

        if (!f.exists()) {
            // Dir doesn't exist. Creating it.
            f.mkdirs();
        } else if (f.isFile()) {
            //TODO
            Log.e(TAG, "Output dir already exists as regular file...", new Throwable());
        }
    }

    String getBaseDir(String dirId) {
        String wDir = pref_workingDir;

        if (pref_writeThumbnailedToOriginalFolder && dirId.equals(SUFFIX_DEST)) {
            wDir = "";
        }

        return getWritablePath() + d + wDir + d + getMainDir() + suffixes.get(dirId);

    }

}
