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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class UriUtil {

    /*
        content://+++/tree/primary:DCIM/d1/e2/document/primary:DCIM/d1/e2/f3

    getTreeId (=getTreeDocumentId)  = primary:DCIM/d1/e2
    getTVolId                       = primary
    getTD1                          = DCIM
    getTDSub                        = d1/e2
    getTParent                      = primary:DCIM/d1
    getTName                        = e2
    getTPath                        = DCIM/d1/e2


    getDocId (=getDocumentId)       = primary:DCIM/d1/e2/f3
    getDVolId                       = primary
    getDD1                          = DCIM
    getDDSub                        = d1/e2/f3
    getDDSubParent                  = d1/e2
    getDParent                      = primary:DCIM/d1/e2
    getDName                        = f3
    getDPath                        = DCIM/d1/e2/f3
    getDPathParent                  = DCIM/d1/e2


    getAsTreeUri                    = content://+++/tree/primary:DCIM/d1/e2
    buildTParentAsTreeUri           = content://+++/tree/primary:DCIM/d1
    buildTParentAsTreeUriWithDocument
    buildDParentAsUri                = content://+++/tree/primary:DCIM/d1/e2/document/primary:DCIM/d1/e2
     */



    public static String getTreeId(Uri uri) {
        return DocumentsContract.getTreeDocumentId(uri);
    }

    public static Uri getAsTreeUri(Uri uri) {
        // Transform 'content://***/tree/***/document/***' to 'content://***/tree/***'
        String treeUri = getTreeId(uri);
        return DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), treeUri);
    }

    public static String getTPath(Uri uri) {
        final String docId = getTreeId(uri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return "";
    }

    public static String getTVolId(Uri uri) {
        return getTreeId(uri).split(":")[0];
    }

    public static String getTD1(Uri uri) {
        // primary:D1/E1 --> D1
        String[] splitTreeId = getTreeId(uri).split(":");
        if (splitTreeId.length > 1)
            return getD1(splitTreeId[1]);
        else
            return splitTreeId[0];
    }

    public static String getD1(String path) {
        String[] splitPath = path.split(File.separator);
        return splitPath[0];
    }

    public static String getTDSub(Uri uri) {
        return getDSub(getTreeId(uri));
    }

    public static String getDSub(String path) {
        String[] splitPath = path.split(File.separator);
        if (splitPath.length == 1)
            return "";
        else
            return String.join(File.separator, Arrays.copyOfRange(splitPath, 1, splitPath.length));
    }

    public static String getTParent(Uri uri) {
        String[] splitPath = getTreeId(uri).split(File.separator);
        if (splitPath.length == 1) // eg. { "primary:D1" }
            return (splitPath[0].split(":")[0] + ":");
        else
            return String.join(File.separator, Arrays.copyOfRange(splitPath, 0, splitPath.length-1));
    }

    public static Uri buildTParentAsTreeUri(Uri uri) {
        // Transform 'content://***/tree/primary:D1/D2/D3/document/***' to 'content://***/tree//primary:D1/D2'
        String treeParent = getTParent(uri);
        return DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), treeParent);
    }

    public static Uri buildTParentAsTreeUriWithDocument(Uri uri) {
        // Probably useless !!
        // Transform 'content://***/tree/primary:D1/D2/D3/document/***' to 'content://***/tree//primary:D1/D2/document/**'
        String treeParent = getTParent(uri);
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), treeParent);
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, getDocId(uri));
    }

    public static String getTName(Uri uri) {
        // Probably useless !!
        String[] splitPath = getTreeId(uri).split(File.separator);
        if (splitPath.length == 1) {
            String[] splitFirstElement = splitPath[0].split(":");
            if (splitFirstElement.length == 1) {
                return "";
            } else {
                return splitFirstElement[1];
            }
        } else {
            return splitPath[splitPath.length-1];
        }
    }


    public static String getDocId(Uri uri) {
        return DocumentsContract.getDocumentId(uri);
    }

    public static String getDPath(Uri uri) {
        final String docId = getDocId(uri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return "";
    }

    public static String getDPathParent(Uri uri) {
        Path path = Paths.get(getDPath(uri));
        Path parent = path.getParent();
        if (parent == null)
            return "";

        return parent.toString();
    }

    public static String getDVolId(Uri uri) {
        return getDocId(uri).split(":")[0];
    }

    public static String getDD1(Uri uri) {
        // primary:D1/E1 --> D1
        String[] splitDocId = getDocId(uri).split(":");
        if (splitDocId.length > 1)
            return getD1(splitDocId[1]);
        else
            return splitDocId[0];
    }

    public static String getDDSub(Uri uri) {
        return getDSub(getDocId(uri));
    }

    public static String getDDSubParent(Uri uri) {
        Path path = Paths.get(getDDSub(uri));
        Path parent = path.getParent();
        if (parent == null)
            return "";

        return parent.toString();
    }

    public static String getDParent(Uri uri) {
        String[] splitPath = getDocId(uri).split(File.separator);
        if (splitPath.length == 1) // eg. { "primary:D1" }
            return (splitPath[0].split(":")[0] + ":");
        else
            return String.join(File.separator, Arrays.copyOfRange(splitPath, 0, splitPath.length-1));
    }

    public static Uri buildDParentAsUri(Uri uri) throws IllegalArgumentException {
        // Transform 'content://***/tree/***/document/primary:D1/E2/F3' to 'content://***/tree/***/document/primary:D1/E2'
        String dParentUri = getDParent(uri);
        String treeId =  getTreeId(uri);

        if (dParentUri.length() < treeId.length())
            throw new IllegalArgumentException("Document Uri is out of tree");

        Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), treeId);
        return DocumentsContract.buildDocumentUriUsingTree(treeRootUri, dParentUri);
    }

    public static String getDName(Uri uri) {
        String[] splitPath = getDocId(uri).split(File.separator);
        if (splitPath.length == 1) {
            String[] splitFirstElement = splitPath[0].split(":");
            if (splitFirstElement.length == 1) {
                return "";
            } else {
                return splitFirstElement[1];
            }
        } else {
            return splitPath[splitPath.length-1];
        }
    }


    public static long getFlags(Context context, Uri self) {
        return queryForLong(context, self, DocumentsContract.Document.COLUMN_FLAGS, 0);
    }

    private static int queryForInt(Context context, Uri self, String column,
                                   int defaultValue) {
        //https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:documentfile/documentfile/src/main/java/androidx/documentfile/provider/DocumentsContractApi19.java
        return (int) queryForLong(context, self, column, defaultValue);
    }

    private static long queryForLong(Context context, Uri self, String column,
                                     long defaultValue) {
        //https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:documentfile/documentfile/src/main/java/androidx/documentfile/provider/DocumentsContractApi19.java
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[] { column }, null, null, null);
            if (c!= null && c.moveToFirst() && !c.isNull(0)) {
                return c.getLong(0);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            if (enableLog) Log.w(TAG, "Failed query: " + e);
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(@Nullable AutoCloseable closeable) {
        //https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:documentfile/documentfile/src/main/java/androidx/documentfile/provider/DocumentsContractApi19.java
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean supportsCopy(Context con, Uri treeUri) {
        // from : https://developer.android.com/reference/android/provider/DocumentsContract.Document#FLAG_SUPPORTS_COPY
        // Constant Value: 128 (0x00000080)

        long flags = getFlags(con, treeUri);
        long tmp = (flags & 128);
        if ((flags & 128) == 128)
            return true;
        else
            return false;
    }
}
