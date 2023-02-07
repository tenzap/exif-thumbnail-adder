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
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.exifthumbnailadder.app.exception.BadOriginalImageException;
import com.schokoladenbrown.Smooth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Set;

import static com.exifthumbnailadder.app.MainApplication.TAG;
import static com.exifthumbnailadder.app.MainApplication.enableLog;

public abstract class ETADoc {
    final String THUMB_EXT = "";
    final String SUFFIX_TMP = "tmp";
    final String SUFFIX_BACKUP = "bak";
    final String SUFFIX_DEST = "dest";
    final String d = File.separator;    // "/"

    final HashMap<String, String> suffixes = new HashMap<String, String>();

    final boolean pref_useSAF;
    final String pref_workingDir;
    final boolean pref_writeTmpToCacheDir;
    final boolean pref_writeThumbnailedToOriginalFolder;

    final Context ctx;
    final String volumeName;
    final String volumeRootPath;
    final ETASrcDir root;
    final boolean withVolumeName;

    boolean toBitmapReturnsRotatedBitmap = false;
    BasicFileAttributes attributeBasic;
    UserPrincipal attributeUser;
    Set<PosixFilePermission> attributePosix;
    boolean attributesAreSet = false;

    int imageWidth = 0;
    int imageHeight = 0;

    protected ETADoc(Context ctx, ETASrcDir root, String volumeName, String volumeRootPath, boolean withVolumeName) {
        this.ctx = ctx;
        this.root = root;
        this.volumeName = volumeName;
        this.volumeRootPath = volumeRootPath;

        this.withVolumeName = withVolumeName;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        pref_useSAF = prefs.getBoolean("useSAF", true);
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
    public abstract String getFullFSPathToBackup();
    public abstract String getFullFSPathToDest();
    public abstract void writeInTmp(ByteArrayOutputStream newImgOs) throws Exception;
    public abstract Uri getOutputFileUri(Uri tmpUri, String filename);
    public abstract boolean isFile();
    public abstract boolean isDirectory();
    public abstract boolean delete();
    public abstract String getDPath();
    public abstract boolean deleteOutputInTmp();

    abstract String getBaseDir(String dirId);
    abstract void copyAttributesTo(Object doc) throws Exception;

    protected abstract void getWidthHeight() throws Exception;

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


    private Bitmap rotateThumbnail(Bitmap tb_bitmap, boolean flip, int degrees) {
        // Google's "Files" app applies the rotation of the principal picture to the thumbnail
        // when it displays the thumbnail. Kde in PTP mode and Windows don't do that, so they have to
        // rotate the thumbnail.
        // Neither GoogleFiles, nor the others consider the "Orientation" tag when set on IFD1
        // (which is for the thumbnail), so it is not usefull to set that orientation tag

        // Get rotation & rotate thumbnail
        Matrix matrix = new Matrix();
        if (degrees < 0) {
            // First undo rotation, then flip
            matrix.postRotate(degrees);
            if (flip) {
                matrix.postScale(-1f, 1f);
            }
        } else {
            // First flip, then do rotation
            if (flip) {
                matrix.postScale(-1f, 1f);
            }
            matrix.postRotate(degrees);
        }
        return Bitmap.createBitmap(tb_bitmap, 0, 0, tb_bitmap.getWidth(), tb_bitmap.getHeight(), matrix, true);
    }

    public Bitmap getThumbnail(String lib, boolean rotateThumbnail, boolean isFlipped, int degrees) throws Exception, BadOriginalImageException {
        ThumbnailFactory tf = new ThumbnailFactory();
        ThumbnailProject tp = new ThumbnailProject(toBitmap());

        Bitmap thumbnail = null;

        switch (lib) {
            case "ThumbnailUtils":
                thumbnail = tf.getThumbnailWithThumbnailUtils(tp);
                break;
            case "ffmpeg":
                // Algorithm choice for ffmpeg swscale: https://stackoverflow.com/a/29743840/15401262
                //  "I'd say the quality is: point << bilinear < bicubic < lanczos/sinc/spline I don't really know the others"
                // Bilinear is somehow blurred
                // Sinc and Lanczos look similar, but Lanczos seems much faster. So choosing this one
                tp.setAlgo1(Smooth.AlgoParametrized1.LANCZOS, 3.0);
                thumbnail = tf.getThumbnailWithFfmpegService(ctx, tp);
                break;
            case "internal":
            default:
                // There is ThumbnailUtils.extractThumbnail in Android, but quality doesn't seem much better.
                // https://stackoverflow.com/a/13252754
                // Apply the principle of not reducing more than 50% each time
                thumbnail = tf.getThumbnailWithCreateScaledBitmap(tp);
                break;
        }

        if (thumbnail == null) {
            if (enableLog) Log.e(TAG, "Couldn't build thumbnails (bitmap is null... abnormal...)");
            //return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            throw new Exception("Couldn't build thumbnails (is null)");
        }

        if (rotateThumbnail) {
            if (!toBitmapReturnsRotatedBitmap) {
                thumbnail = rotateThumbnail(thumbnail, isFlipped, degrees);
            }
        } else {
            if (toBitmapReturnsRotatedBitmap) {
                // we need to undo rotation
                thumbnail = rotateThumbnail(thumbnail, isFlipped, -degrees);
            }
        }

        return thumbnail;
    }

    void storeFileAttributes() throws Exception {
        Path inFilePath = Paths.get(getFullFSPath());
        try {
            // Save basic attributes (containing file timestamps)
            attributeBasic = Files.readAttributes(inFilePath, BasicFileAttributes.class);
            // Save owner attribute
            attributeUser = Files.getOwner(inFilePath, LinkOption.NOFOLLOW_LINKS);
            // Save Posix attributes
            attributePosix = Files.getPosixFilePermissions(inFilePath, LinkOption.NOFOLLOW_LINKS);
            attributesAreSet = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    public int getWidth() throws Exception {
        getWidthHeight();
        return this.imageWidth;
    }

    public int getHeight() throws Exception {
        getWidthHeight();
        return this.imageHeight;
    }
}
