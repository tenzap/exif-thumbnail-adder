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

public class ETADoc {
    private final String THUMB_EXT = "";

    DocumentFile docFile = null;
    Uri uri = null;
    File file = null;
    String type;
    PathUtil pathUtil = null;
    Context ctx;
    String volumeRootPath;
    String volumeName;
    ETADocs root;
    boolean withVolumeName;

    public ETADoc(DocumentFile docFile, Context ctx, ETADocs root, boolean withVolumeName) {
        type = "Uri";
        this.docFile = docFile;
        this.uri = docFile.getUri();
        this.ctx = ctx;
        this.volumeName = root.getVolumeName();
        this.root = root;
        this.withVolumeName = withVolumeName;

        pathUtil = new PathUtil(
                uri,
                getMainDir(),
                getSubDir(),
                this.volumeName,
                root.getSecStorageDirName(),
                PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    public ETADoc(File file, Context ctx, ETADocs root, boolean withVolumeName) {
        this.file = file;
        type = "File";
        this.ctx = ctx;
        this.volumeRootPath = root.getVolumeRootPath();
        this.volumeName = root.getVolumeName();
        this.root = root;
        this.withVolumeName = withVolumeName;

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
                    if (b.length > 2)
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

    public Uri getTmpUri() {
        switch (type) {
            case "File":
                throw new UnsupportedOperationException();
            case "Uri":
                return pathUtil.getTmpUri(ctx, withVolumeName);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Uri getBackupUri() {
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

    private String getTmpDir() {
        switch (type) {
            case "File":
                return pathUtil.getTmpDir(ctx, withVolumeName);
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Path getTmpPath() {
        switch (type) {
            case "File":
                return new File(getTmpDir()).toPath();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    private String getBackupDir() {
        switch (type) {
            case "File":
                return pathUtil.getBackupDir(withVolumeName);
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Path getBackupPath() {
        switch (type) {
            case "File":
                return new File(getBackupDir()).toPath();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    private String getDestDir() {
        switch (type) {
            case "File":
                return pathUtil.getDestDir();
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Path getDestPath() {
        switch (type) {
            case "File":
                return new File(getDestDir()).toPath();
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
        switch (type) {
            case "File":
                return file;
            case "Uri":
                throw new UnsupportedOperationException();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public InputStream inputStream() throws Exception {
        switch (type) {
            case "File":
                try {
                    return new FileInputStream(file);
                } catch (Exception e) {
                    throw e;
                }
            case "Uri":
                try {
                    return ctx.getContentResolver().openInputStream(uri);
                } catch (Exception e) {
                    throw e;
                }
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

    public void createDirForTmp() {
        switch (type) {
            case "File":
                PathUtil.createDirFor(getTmpDir());
                return;
            case "Uri":
                PathUtil.createDirFor(ctx, getTmpUri());
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void createDirForBackup() {
        switch (type) {
            case "File":
                PathUtil.createDirFor(getBackupDir());
                return;
            case "Uri":
                PathUtil.createDirFor(ctx, getBackupUri());
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void createDirForDest() {
        switch (type) {
            case "File":
                PathUtil.createDirFor(getDestDir());
                return;
            case "Uri":
                PathUtil.createDirFor(ctx, getDestUri());
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Object getOutputInTmp() {
        switch (type) {
            case "File":
                Path outputFilename = getTmpPath().resolve(getName() + THUMB_EXT);
                return outputFilename.toFile(); // File
            case "Uri":
                String filename = getName() + THUMB_EXT;
                Uri outputTmpFileUri = getOutputFileUri(getTmpUri(), filename);
                return outputTmpFileUri; // Uri
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getTmpFSPathWithFilename() {
        switch (type) {
            case "File":
                return ((File)getOutputInTmp()).getPath();
            case "Uri":
                String outFilepath;
                Uri outputTmpFileUri = (Uri)getOutputInTmp();
                if ( outputTmpFileUri.getScheme().equals("file")) {
                    outFilepath = outputTmpFileUri.getPath();
                } else {
                    outFilepath = FileUtil.getFullDocIdPathFromTreeUri(outputTmpFileUri, ctx);
                }
                return outFilepath;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getFullFSPath() {
        switch (type) {
            case "File":
                return file.getPath();
            case "Uri":
                return FileUtil.getFullDocIdPathFromTreeUri(uri, ctx);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void writeInTmp(ByteArrayOutputStream newImgOs) throws Exception {
        switch (type) {
            case "File":
                File outputFilename = (File) getOutputInTmp();
                if (enableLog) Log.i(TAG, "Write to: " + outputFilename);
                FileOutputStream outputStream;
                outputStream = new FileOutputStream(outputFilename);
                outputStream.write(newImgOs.toByteArray());
                outputStream.close();
                if (enableLog) Log.i(TAG, "Write to DONE: " + outputFilename);
                return;
            case "Uri":
                Uri outputTmpFileUri = (Uri) getOutputInTmp();
                OutputStream outputStream2 = null;
                outputStream2 = ctx.getContentResolver().openOutputStream(outputTmpFileUri);
                outputStream2.write(newImgOs.toByteArray());
                outputStream2.close();
                if (enableLog) Log.i(TAG, "Write to DONE");
                return;
            default:
                throw new UnsupportedOperationException();
        }
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
        switch (type) {
            case "File":
                return file.isFile();
            case "Uri":
                return DocumentFile.fromTreeUri(ctx, uri).isFile();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public boolean isDirectory() {
        switch (type) {
            case "File":
                return file.isDirectory();
            case "Uri":
                return DocumentFile.fromTreeUri(ctx, uri).isDirectory();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public boolean delete() {
        switch (type) {
            case "File":
                return file.delete();
            case "Uri":
                return DocumentFile.fromTreeUri(ctx, uri).delete();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public String getDPath() {
        switch (type) {
            case "File":
                return file.toString().substring(volumeRootPath.length());
            case "Uri":
                return UriUtil.getDPath(uri);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Path getSrcPath(String srcFSPath) {
        if (type != "File")
            throw new UnsupportedOperationException();

        String tmp = file.getPath(); // "/storage/emulated/0/ThumbAdder/DCIM.bak/external_primary/dir1/s2/image.jpg

        tmp = tmp.substring(volumeRootPath.length()); // "/ThumbAdder/DCIM.bak/external_primary/dir1/s2/image.jpg

        if (withVolumeName) {
            // remove the volumeName from the path
            tmp = tmp.replace(File.separator + volumeName + File.separator, File.separator); // "/ThumbAdder/DCIM.bak/dir1/s2/image.jpg"
        }

        tmp = UriUtil.getDSub(UriUtil.getDSub(UriUtil.getDSub(tmp))); // "dir1/s2/image.jpg"

        return new File(srcFSPath).toPath().resolve(tmp);
    }

    public Uri getSrcUri(String srcDirMainDir, String srcDirTreeId) throws Exception {
        if (type != "Uri")
            throw new UnsupportedOperationException();
        try {
            return PathUtil.getSrcDocumentUriFor(uri, srcDirMainDir, srcDirTreeId, withVolumeName);
        } catch (Exception e) {
            throw e;
        }
    }

}
