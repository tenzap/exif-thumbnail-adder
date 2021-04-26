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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Size;

import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

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
    public abstract Bitmap toBitmap() throws Exception;
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
    public abstract boolean deleteOutputInTmp();

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

    public static boolean isImageFile(String path) {
        // https://stackoverflow.com/a/30696106
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    public static boolean srcUriCorrespondsDerivedUri(Uri srcUri, Uri derivedUri) {
        String subSrc = UriUtil.getDDSub(srcUri);
        String subDerived = UriUtil.getDSub((UriUtil.getDDSub(derivedUri)));
        return subSrc.equals(subDerived);
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

    private static Size getThumbnailTargetSize(int imageWidth, int imageHeight, int maxSize) {
        float imageRatio = ((float)Math.min(imageWidth, imageHeight) / (float)Math.max(imageWidth, imageHeight));
        int thumbnailWidth = (imageWidth < imageHeight) ? Math.round(maxSize*imageRatio) : maxSize ;
        int thumbnailHeight = (imageWidth < imageHeight) ? maxSize : Math.round(maxSize*imageRatio);
//        if (imageWidth < imageHeight) {
//            // Swap thumbnail width and height to keep a relative aspect ratio
//            int temp = thumbnailWidth;
//            thumbnailWidth = thumbnailHeight;
//            thumbnailHeight = temp;
//        }
        if (imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
        if (imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;

        return new Size(thumbnailWidth, thumbnailHeight);
    }

    private static Bitmap createThumbnail(InputStream is) throws FirstFragment.BadOriginalImageException {
        Bitmap original = BitmapFactory.decodeStream(is);

        if (original == null) {
            throw new FirstFragment.BadOriginalImageException();
        }
        int imageWidth = original.getWidth();
        int imageHeight = original.getHeight();

        Size targetSize = getThumbnailTargetSize(imageWidth, imageHeight, 160);

        // https://stackoverflow.com/a/13252754
        // Apply the principle of not reducing more than 50% each time
        int tmpWidth = imageWidth;
        int tmpHeight = imageHeight;
        Bitmap thumbnail = original;
        while (tmpWidth / targetSize.getWidth() > 2 || tmpHeight / targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = Bitmap.createScaledBitmap(thumbnail, tmpWidth, tmpHeight, true);
        }
        thumbnail = Bitmap.createScaledBitmap(thumbnail, targetSize.getWidth(), targetSize.getHeight(), true);

        return thumbnail;
    }

    private Bitmap rotateThumbnail(Bitmap tb_bitmap, int degrees) {
        // Google's "Files" app applies the rotation of the principal picture to the thumbnail
        // when it displays the thumbnail. Kde in PTP mode and Windows don't do that, so the have to
        // rotate the thumbnail.
        // Neither GoogleFiles, nor the others consider the "Orientation" tag when set on IFD1
        // (which is for the thumbnail), so it is not usefsull to set that orientation tag

        // Get rotation & rotate thumbnail
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(tb_bitmap, 0, 0, tb_bitmap.getWidth(), tb_bitmap.getHeight(), matrix, true);
    }

    public Bitmap getThumbnail(boolean rotateThumbnail, int degrees) throws Exception, FirstFragment.BadOriginalImageException {
        // There is ThumbnailUtils.extractThumbnail in Android, but quality doesn't seem much better.
        Bitmap tb_bitmap = null;
        InputStream is = null;
        try {
            is = inputStream();
            tb_bitmap = createThumbnail(is);
            is.close();
        } catch (FirstFragment.BadOriginalImageException e) {
            throw e;
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            throw e;
        }

        if (tb_bitmap != null && rotateThumbnail) {
            tb_bitmap = rotateThumbnail(tb_bitmap, degrees);
        }
        if (tb_bitmap != null) {
            return tb_bitmap;
        } else {
            if (enableLog) Log.e(TAG, "Couldn't build thumbnails (bitmap is null... abnormal...)");
            //return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            throw new Exception("Couldn't build thumbnails (is null)");
        }
    }

    public Bitmap getThumbnailUsingThumbnailUtils(boolean rotateThumbnail, int degrees) throws Exception, FirstFragment.BadOriginalImageException {
        Bitmap original = toBitmap();
        if (original == null) {
            throw new FirstFragment.BadOriginalImageException();
        }
        int imageWidth = original.getWidth();
        int imageHeight = original.getHeight();

        Size targetSize = getThumbnailTargetSize(imageWidth, imageHeight, 160);

        int tmpWidth = imageWidth;
        int tmpHeight = imageHeight;
        Bitmap thumbnail = original;
        while (tmpWidth / targetSize.getWidth() > 2 || tmpHeight / targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, tmpWidth, tmpHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, targetSize.getWidth(), targetSize.getHeight(), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        if (thumbnail != null && rotateThumbnail) {
            thumbnail = rotateThumbnail(thumbnail, degrees);
        }
        if (thumbnail != null) {
            return thumbnail;
        } else {
            if (enableLog) Log.e(TAG, "Couldn't build thumbnails (bitmap is null... abnormal...)");
            //return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            throw new Exception("Couldn't build thumbnails (is null)");
        }
    }
}
