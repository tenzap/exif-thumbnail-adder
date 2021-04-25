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
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

public class ETADocDf extends ETADoc {

    DocumentFile etaDoc = null;
    Uri _uri = null;

    public ETADocDf(DocumentFile docFile, Context ctx, ETADocs root, boolean withVolumeName) {
        this.etaDoc = docFile;
        this._uri = docFile.getUri();

        this.ctx = ctx;
        this.volumeName = root.getVolumeName();
        this.root = root;
        this.withVolumeName = withVolumeName;

        pathUtil = new PathUtil(
                _uri,
                getMainDir(),
                getSubDir(),
                this.volumeName,
                root.getSecStorageDirName(),
                PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    public String getMainDir() {
        return UriUtil.getDD1(_uri);
    }

    public String getSubDir() {
        return UriUtil.getDDSubParent(_uri);
    }

    public String getTreeId() {
        return UriUtil.getTreeId(_uri);
    }

    public boolean exists() {
        return etaDoc.exists();
    }

    public boolean isJpeg() {
        return etaDoc.getType().equals("image/jpeg");
    }

    public long length() {
        return etaDoc.length();
    }

    public String getName() {
        return etaDoc.getName();
    }

    public Uri getTmpUri() {
        return pathUtil.getTmpUri(ctx, withVolumeName);
    }

    public Uri getBackupUri() {
        return pathUtil.getBackupUri(ctx, withVolumeName);
    }

    public Uri getDestUri() {
        return pathUtil.getDestUri(ctx);
    }

    public Path getTmpPath() {
        throw new UnsupportedOperationException();
    }

    public Path getBackupPath() {
        throw new UnsupportedOperationException();
    }

    public Path getDestPath() {
        throw new UnsupportedOperationException();
    }

    public Uri getUri() {
        return _uri;
    }

    public InputStream inputStream() throws Exception {
        try {
            return ctx.getContentResolver().openInputStream(_uri);
        } catch (Exception e) {
            throw e;
        }
    }

    public Path toPath() {
        throw new UnsupportedOperationException();
    }

    public String getWritablePath() {
        throw new UnsupportedOperationException();
    }

    public void createDirForTmp() {
        PathUtil.createDirFor(ctx, getTmpUri());
    }

    public void createDirForBackup() {
        PathUtil.createDirFor(ctx, getBackupUri());
    }

    public void createDirForDest() {
        PathUtil.createDirFor(ctx, getDestUri());
    }

    public Object getOutputInTmp() {
        String filename = getName() + THUMB_EXT;
        Uri outputTmpFileUri = getOutputFileUri(getTmpUri(), filename);
        return outputTmpFileUri; // Uri
    }

    public String getTmpFSPathWithFilename() {
        String outFilepath;
        Uri outputTmpFileUri = (Uri)getOutputInTmp();
        if ( outputTmpFileUri.getScheme().equals("file")) {
            outFilepath = outputTmpFileUri.getPath();
        } else {
            outFilepath = FileUtil.getFullDocIdPathFromTreeUri(outputTmpFileUri, ctx);
        }
        return outFilepath;
    }

    public String getFullFSPath() {
        return FileUtil.getFullDocIdPathFromTreeUri(_uri, ctx);
    }

    public void writeInTmp(ByteArrayOutputStream newImgOs) throws Exception {
        Uri outputTmpFileUri = (Uri) getOutputInTmp();
        OutputStream outputStream2 = null;
        outputStream2 = ctx.getContentResolver().openOutputStream(outputTmpFileUri);
        outputStream2.write(newImgOs.toByteArray());
        outputStream2.close();
        if (enableLog) Log.i(TAG, "Write to DONE");
    }

    public Uri getOutputFileUri(Uri tmpUri, String filename) {
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
        return DocumentFile.fromTreeUri(ctx, _uri).isFile();
    }

    public boolean isDirectory() {
        return DocumentFile.fromTreeUri(ctx, _uri).isDirectory();
    }

    public boolean delete() {
        return DocumentFile.fromTreeUri(ctx, _uri).delete();
    }

    public String getDPath() {
        return UriUtil.getDPath(_uri);
    }

    public Path getSrcPath(String srcFSPath) {
        throw new UnsupportedOperationException();
    }

    public Uri getSrcUri(String srcDirMainDir, String srcDirTreeId) throws Exception {
        try {
            return PathUtil.getSrcDocumentUriFor(_uri, srcDirMainDir, srcDirTreeId, withVolumeName);
        } catch (Exception e) {
            throw e;
        }
    }


}
