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
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

public class PathUtil {
    public static final String SUFFIX_TMP = "tmp";
    public static final String SUFFIX_BACKUP = "bak";
    public static final String SUFFIX_DEST = "dest";

    private final String storagePath;         // /storage/emulated/0   or   /storage/xxxx-xxxx
                                        // or in case of URI: "primary:" or "xxxx-xxxx:"
    private String workingDir;          // ThumbAdder
    private String mainDir;             // First dir at root of volume (eg. DCIM or Pictures)
    private String subDir;              // subdir1/subdir2
    private String volumeName;          // MediaStore.VOLUME_EXTERNAL_PRIMARY   or   xxxx-xxxx (lower case)
    private String secStorageDirName;   // du type xxxx-xxxx (lower case)
    private String uriAuthority;
    private String sourceFileTreeId;

    private boolean writeTmpToCacheDir = true;
    private boolean writeThumbnailedToOriginalFolder = false;

    private final String d = File.separator;    // "/"

    private final HashMap<String, String> suffixes = new HashMap<String, String>();

    public PathUtil(String storagePath, String mainDir, String subDir, String volumeName, String secStorageDirName, SharedPreferences prefs) {
        init(prefs, mainDir, subDir, volumeName, secStorageDirName);

        this.storagePath = storagePath;
    }

    public PathUtil(Uri contentUri, String mainDir, String subDir, String volumeName, String secStorageDirName, SharedPreferences prefs) {
        init(prefs, mainDir, subDir, volumeName, secStorageDirName);

        this.storagePath =  UriUtil.getDVolId(contentUri) + ":";   // "primary:"
        this.uriAuthority = contentUri.getAuthority();
        this.sourceFileTreeId = UriUtil.getTreeId(contentUri);
    }

    private void init(SharedPreferences prefs, String mainDir, String subDir, String volumeName, String secStorageDirName) {
        this.workingDir = prefs.getString("working_dir", "ThumbAdder");
        writeTmpToCacheDir = prefs.getBoolean("writeTmpToCacheDir", true);
        writeThumbnailedToOriginalFolder = prefs.getBoolean("writeThumbnailedToOriginalFolder", false);

        this.mainDir = mainDir;
        this.subDir = subDir;
        this.volumeName = volumeName;
        this.secStorageDirName = secStorageDirName;

        if (writeThumbnailedToOriginalFolder) {
            suffixes.put(SUFFIX_DEST,"");
        } else {
            suffixes.put(SUFFIX_DEST,".new");
        }
        suffixes.put(SUFFIX_TMP,".tmp");
        suffixes.put(SUFFIX_BACKUP,".bak");
    }

    public String getTmpDir(Context con, boolean withVolumeName) {
        String dirId = SUFFIX_TMP;
        String baseDir, fullDir;
        
        if (writeTmpToCacheDir) {
            baseDir = con.getExternalCacheDir() + d + mainDir + suffixes.get(dirId);
        } else {
            baseDir = getBaseDir(dirId);
        }

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        PathUtil.createNomediaFile(baseDir, storagePath + d + mainDir);

        Log.i("MyLog", "Writing files for '" + dirId + "' to: " + fullDir);
        return fullDir;
    }

    public Uri getTmpUri(Context con, boolean withVolumeName) {
        String dirId = SUFFIX_TMP;
        String baseDir, fullDir;

        if (writeTmpToCacheDir) {
            baseDir = con.getExternalCacheDir() + d + mainDir + suffixes.get(dirId);
        } else {
            baseDir = getBaseDir(dirId);
        }

        fullDir = getFullDir(baseDir, withVolumeName);

        Uri treeRootUri = null;
        Uri outUri = null;

        if (!writeTmpToCacheDir) {
            // Remove trailing "/"
            baseDir = Paths.get(baseDir).toString();
            fullDir = Paths.get(fullDir).toString();

            treeRootUri = DocumentsContract.buildTreeDocumentUri(uriAuthority, storagePath+workingDir);
            outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

            PathUtil.createNomediaFile(con, treeRootUri, baseDir, mainDir);
        } else {
            outUri = Uri.fromFile(new File(fullDir));
            PathUtil.createNomediaFile(baseDir, con.getExternalCacheDir() + d + mainDir);
        }

        Log.i("MyLog", "Writing files for '" + dirId + "' to: " + fullDir);
        return outUri;
    }

    public String getBackupDir(boolean withVolumeName) {
        String dirId = SUFFIX_BACKUP;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        PathUtil.createNomediaFile(baseDir, storagePath + d + mainDir);

        Log.i("MyLog", "Writing files for '" + dirId + "' to: " + fullDir);
        return fullDir;
    }

    public Uri getBackupUri(Context con, boolean withVolumeName) {
        String dirId = SUFFIX_BACKUP;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, withVolumeName);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uriAuthority, storagePath+workingDir);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        PathUtil.createNomediaFile(con, treeRootUri, baseDir, mainDir);
        return outUri;
    }

    public String getDestDir() {
        String dirId = SUFFIX_DEST;
        String baseDir, fullDir;

        baseDir = getBaseDir(dirId);

        if (volumeName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY)) {
            fullDir = getFullDir(baseDir, false);
        } else {
            fullDir = baseDir + d + secStorageDirName + d + subDir;
        }

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        if (! suffixes.get(dirId).isEmpty())
            PathUtil.createNomediaFile(baseDir, storagePath + d + mainDir);
        return fullDir;
    }

    public Uri getDestUri(Context con) {
        String dirId = SUFFIX_DEST;
        String baseDir, fullDir, treeDocId;

        baseDir = getBaseDir(dirId);

        fullDir = getFullDir(baseDir, false);

        // Remove trailing "/"
        baseDir = Paths.get(baseDir).toString();
        fullDir = Paths.get(fullDir).toString();

        if (writeThumbnailedToOriginalFolder) {
            // We want to write to the source tree
            treeDocId = sourceFileTreeId;
        } else {
            // We want to write to the workingDir tree
            treeDocId = storagePath+workingDir;
        }
        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uriAuthority, treeDocId);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        if (! suffixes.get(dirId).isEmpty()) {
            PathUtil.createNomediaFile(con, treeRootUri, baseDir, mainDir);
        }
        return outUri;
    }

    public static Uri getSrcDocumentUriFor(Uri contentUri, String mainDir, String sourceFileTreeIdForGetSrcUri) throws Exception {
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

        fullDir = baseDir + File.separator + UriUtil.getDSub(UriUtil.getDDSub(contentUri));

        // Remove trailing "/"
        fullDir = Paths.get(fullDir).toString();

        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uriAuthority, sourceFileTreeIdForGetSrcUri);
        Uri outUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, fullDir);

        return outUri;
    }

    private String getBaseDir(String dirId) {
        String wDir = workingDir;

        if (writeThumbnailedToOriginalFolder && dirId.equals(SUFFIX_DEST)) {
            wDir = "";
        }

        if (storagePath.endsWith(":")) {
            // This is a URI path (primary:DCIM....)
            return storagePath + wDir + d + mainDir + suffixes.get(dirId);
        } else {
            return storagePath + d + wDir + d + mainDir + suffixes.get(dirId);
        }
    }

    private String getFullDir(String baseDir, boolean withVolumeName) {
        if (withVolumeName) {
            return baseDir + d + volumeName + d + subDir;
        } else {
            return baseDir + d + subDir;
        }
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

    public static void createNomediaFile(Context con, Uri treeRootUri, String mainDir, String exceptDir) {
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

    public static void createDirFor(String path) {
        File f = new File(path);

        if (!f.exists()) {
            // Dir doesn't exist. Creating it.
            f.mkdirs();
        } else if (f.isFile()) {
            //TODO
            Log.e("MyLog", "Output dir already exists as regular file...", new Throwable());
        }
    }

    public static void createDirFor(Context con, Uri uri) {
        if ( uri.getScheme().equals("file")) {
            createDirFor(uri.getPath());
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

}
