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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

public class ETADocDf extends ETADoc {

    final DocumentFile etaDoc;
    final Uri _uri;
    final String _uriAuthority;

    public ETADocDf(DocumentFile docFile, Context ctx, ETASrcDirUri root, boolean withVolumeName) {
        super(ctx, root, root.getVolumeName(), root.getVolumeRootPath(), withVolumeName);
        this.etaDoc = docFile;
        this._uri = docFile.getUri();
        this._uriAuthority = _uri.getAuthority();
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
        String dirId = SUFFIX_TMP;
        String baseDir, fullDir;

        if (pref_writeTmpToCacheDir) {
            baseDir = ctx.getExternalCacheDir() + d + getMainDir() + suffixes.get(dirId);
        } else {
            baseDir = getBaseDir(dirId);
        }

        fullDir = getFullDir(baseDir, withVolumeName);

        Uri treeRootUri = null;
        Uri outUri = null;

        if (!pref_writeTmpToCacheDir) {
            // Remove trailing "/"
            baseDir = Paths.get(baseDir).toString();
            fullDir = Paths.get(fullDir).toString();

            treeRootUri = DocumentsContract.buildTreeDocumentUri(_uriAuthority, volumeRootPath+pref_workingDir);
            outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

            createNomediaFile(ctx, treeRootUri, baseDir, getMainDir());
        } else {
            outUri = Uri.fromFile(new File(fullDir));
            ETADocFile.createNomediaFile(baseDir, ctx.getExternalCacheDir() + d + getMainDir());
        }

        if (enableLog) Log.i(TAG, "Writing files for '" + dirId + "' to: " + fullDir);
        return outUri;
    }

    public Uri getBackupUri() {
        String dirId = SUFFIX_BACKUP;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(_uriAuthority, volumeRootPath+pref_workingDir);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        createNomediaFile(ctx, treeRootUri, baseDir, getMainDir());
        return outUri;
    }

    public Uri getDestUri() {
        String dirId = SUFFIX_DEST;
        String baseDir, fullDir, treeDocId;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, false);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        if (pref_writeThumbnailedToOriginalFolder) {
            // We want to write to the source tree
            treeDocId = getTreeId();
        } else {
            // We want to write to the workingDir tree
            treeDocId = volumeRootPath+pref_workingDir;
        }
        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(_uriAuthority, treeDocId);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        if (! suffixes.get(dirId).isEmpty()) {
            createNomediaFile(ctx, treeRootUri, baseDir, getMainDir());
        }
        return outUri;
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
        createDirFor(ctx, getTmpUri());
    }

    public void createDirForBackup() {
        createDirFor(ctx, getBackupUri());
    }

    public void createDirForDest() {
        createDirFor(ctx, getDestUri());
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
            return getSrcDocumentUriFor(_uri, srcDirMainDir, srcDirTreeId, withVolumeName);
        } catch (Exception e) {
            throw e;
        }
    }


    private static void createNomediaFile(Context con, Uri treeRootUri, String mainDir, String exceptDir) {
        Uri nomediaParentUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, mainDir);
        Uri nomediaFileUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, mainDir+File.separator+".nomedia");
        DocumentFile nomediaFile = DocumentFile.fromTreeUri(con, nomediaFileUri);

        if (nomediaFile.exists())
            return;

        String path = mainDir.split(":")[1];
        if (path.equals(exceptDir)) {
            return;
        }

        createDirFor(con, nomediaParentUri);

        try {
            DocumentsContract.createDocument(
                    con.getContentResolver(),
                    nomediaParentUri,
                    "",
                    ".nomedia");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createDirFor(Context con, Uri uri) {
        if ( uri.getScheme().equals("file")) {
            ETADocFile.createDirFor(uri.getPath());
            return;
        }

        DocumentFile df = DocumentFile.fromTreeUri(con, uri);
        if (df.exists() && df.isDirectory()) {
            return;
        }

        if (df.isFile()) {
            Log.e("MyLog", "destination dir already exists as file.");
            return;
        }

        String name = UriUtil.getDName(uri);
        Uri parentUri = UriUtil.buildDParentAsUri(uri);
        createDirFor(con, parentUri);
        DocumentFile.fromTreeUri(con, parentUri).createDirectory(name);

    }

    public static boolean srcUriCorrespondsDerivedUri(Uri srcUri, Uri derivedUri) {
        String subSrc = UriUtil.getDDSub(srcUri);
        String subDerived = UriUtil.getDSub((UriUtil.getDDSub(derivedUri)));
        return subSrc.equals(subDerived);
    }

    String getBaseDir(String dirId) {
        String wDir = pref_workingDir;

        if (pref_writeThumbnailedToOriginalFolder && dirId.equals(SUFFIX_DEST)) {
            wDir = "";
        }

        return volumeRootPath + wDir + d + getMainDir() + suffixes.get(dirId);
    }
}
