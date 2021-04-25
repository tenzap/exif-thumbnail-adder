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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import it.sephiroth.android.library.exif2.IfdId;
import it.sephiroth.android.library.exif2.Rational;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class FirstFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
    Handler mHandler;
    public final static SpannableStringBuilder log = new SpannableStringBuilder("");
    ScrollView scrollview = null;
    private boolean stopProcessing = false;
    private boolean isProcessing = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Context fragmentContext = (MainActivity) view.getContext();

        /*view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });*/
        view.findViewById(R.id.button_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSettingsActivity(view );
            }
        });
        view.findViewById(R.id.button_addThumbs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button start = (Button) getView().findViewById(R.id.button_addThumbs);
                Button stop = (Button) getView().findViewById(R.id.button_stopProcess);

                start.setVisibility(Button.GONE);
                stop.setVisibility(Button.VISIBLE);
                addThumbsUsingTreeUris(view);
            }
        });
        view.findViewById(R.id.button_stopProcess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProcessing = true;
                Button start = (Button) getView().findViewById(R.id.button_addThumbs);
                Button stop = (Button) getView().findViewById(R.id.button_stopProcess);
                start.setVisibility(Button.VISIBLE);
                stop.setVisibility(Button.GONE);
            }
        });

        textViewLog = (TextView)view.findViewById(R.id.textview_log);
        textViewDirList = (TextView)view.findViewById(R.id.textview_dir_list);
        scrollview = ((ScrollView)  view.findViewById(R.id.scrollview));
        FirstFragment.updateTextViewDirList(getContext(), textViewDirList);

        LinearLayout ll = (LinearLayout)view.findViewById(R.id.block_allFilesAccess);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use of "All Files Access Permissions" may result in rejection from the google play store
            // We use it only to be able to update the attributes of the files (ie timestamps)
            if (SettingsActivity.haveAllFilesAccessPermission())
                ll.setVisibility(View.GONE);
            else
                ll.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.GONE);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("srcUris")) {
            FirstFragment.updateTextViewDirList(getContext(), textViewDirList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        textViewLog.setText(log);
    }

    public static Bitmap createThumbnail(InputStream is) throws BadOriginalImageException {
        Bitmap original = null;
        original = BitmapFactory.decodeStream(is);

        if (original == null) {
            throw new BadOriginalImageException();
        }
        int imageWidth = original.getWidth();
        int imageHeight = original.getHeight();

        float imageRatio = ((float)Math.min(imageWidth, imageHeight) / (float)Math.max(imageWidth, imageHeight));
        int thumbnailWidth = (imageWidth < imageHeight) ? Math.round(160*imageRatio) : 160 ;
        int thumbnailHeight = (imageWidth < imageHeight) ? 160 : Math.round(160*imageRatio);
//        if (imageWidth < imageHeight) {
//            // Swap thumbnail width and height to keep a relative aspect ratio
//            int temp = thumbnailWidth;
//            thumbnailWidth = thumbnailHeight;
//            thumbnailHeight = temp;
//        }
        if (imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
        if (imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;

        // https://stackoverflow.com/a/13252754
        // Apply the principle of not reducing more than 50% each time
        int tmpWidth = imageWidth;
        int tmpHeight = imageHeight;
        Bitmap thumbnail = original;
        while (tmpWidth / thumbnailWidth > 2 || tmpHeight / thumbnailHeight > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = Bitmap.createScaledBitmap(thumbnail, tmpWidth, tmpHeight, true);
        }
        thumbnail = Bitmap.createScaledBitmap(thumbnail, thumbnailWidth, thumbnailHeight, true);

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

    private Bitmap makeThumbnailRotated(ETADoc pic, boolean rotateThumbnail, int degrees) throws Exception, BadOriginalImageException {
        // pic Object should be either DocumentFile or File
        Bitmap tb_bitmap = null;
        InputStream is = null;
        try {
            is = pic.inputStream();
            tb_bitmap = this.createThumbnail(is);
            is.close();
        } catch (BadOriginalImageException e) {
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
            Log.e(TAG, "Couldn't build thumbnails (bitmap is null... abnormal...)");
            //return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            throw new Exception("Couldn't build thumbnails (is null)");
        }
    }

    private static class MyFilenameFilter implements FilenameFilter {

        private String acceptedPath;
        private boolean recurse;

        public MyFilenameFilter(String acceptedPath, boolean recurse) {
            this.acceptedPath = acceptedPath;
            this.recurse = recurse;
        }

        //apply a filter
        @Override
        public boolean accept(File dir, String name) {
            boolean result;
            //if (true) {
            String dirTrailing = dir.toString() + File.separator;
            boolean recurseCheckAccept = true;
            //if (!recurse && new File(dirTrailing + name).isDirectory()) {
            if (new File(dirTrailing + name).isDirectory()) {
                recurseCheckAccept = false;
            }
            //Log.i("MyLog Here", "D: " + dir.toString() + " N: " + name + " " + dirTrailing + " recurseCheckAccept? " + recurseCheckAccept);

            if (dirTrailing.equals(this.acceptedPath) && recurseCheckAccept) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }
    }

    private String[] getVolumesDir() {
        ArrayList<String> volumesArrayList = new ArrayList<String>();
        String[] volumesArray = new String[volumesArrayList.size()];

        StorageManager myStorageManager = (StorageManager) getActivity().getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> mySV = myStorageManager.getStorageVolumes();
        Class<?> storageVolumeClazz = null;

        for (int i = 0; i < mySV.size(); i++) {
            try {
                storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                Method getPath = storageVolumeClazz.getMethod("getPath");
                volumesArrayList.add((String) getPath.invoke(mySV.get(i)));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            if (enableLog) Log.i(TAG, mySV.get(i).toString());

        }
        volumesArray = volumesArrayList.toArray(volumesArray);
        return volumesArray;
    }


    public static boolean isImageFile(String path) {
        // https://stackoverflow.com/a/30696106
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }


    private void copyFileAttributes(Path inFilePath, Path outFilePath) throws Exception {
        if (enableLog) Log.i(TAG, getString(R.string.frag1_log_copying_attr));
        try {
            BasicFileAttributes inAttrs = Files.readAttributes(inFilePath, BasicFileAttributes.class);

            // Copy owner attribute
            UserPrincipal user = Files.getOwner(inFilePath, LinkOption.NOFOLLOW_LINKS);
            Files.setOwner(outFilePath, user);

            // Copy time attributes
            Files.getFileAttributeView(outFilePath, BasicFileAttributeView.class).setTimes(inAttrs.lastModifiedTime(), inAttrs.lastAccessTime(), inAttrs.creationTime());

            // Copy Posix attributes
            Set<PosixFilePermission> inPosix = Files.getPosixFilePermissions(inFilePath, LinkOption.NOFOLLOW_LINKS);
            Files.setPosixFilePermissions(outFilePath, inPosix);

        } catch (Exception e) {
            e.printStackTrace();
            throw new CopyAttributesFailedException(e);
        }
    }

    private void copyFileAttributes(Uri sourceUri, Uri targetUri) throws Exception {
        /*
           We can't do it through SAF.
           But we can switch to "filesystem" mode

           It seems it is possible to update the attributes even on secondary external storage
           (like sdcard) although I thought we could only write on external_primary, and we could
           not write on secondary volumes (ie. sdcard)
         */

        if (targetUri == null)
            return;

        // Don't do anything if volume root id is not "primary"
        // NOT NEEDED WITH ANDROID 10 BECAUSE IT WORKS ALSO WITH 2ND EXT STORAGE
        //if (!FileUtil.getVolumeIdFromTreeUri(sourceUri).equals("primary"))
        //    return;

        Path inFilePath = null;
        Path outFilePath = null;

        if (sourceUri.getScheme().equals("file")) {
            inFilePath = Paths.get(sourceUri.getPath());
        } else {
            inFilePath = Paths.get(FileUtil.getFullDocIdPathFromTreeUri(sourceUri, getContext()));
        }

        if (targetUri.getScheme().equals("file")) {
            outFilePath = Paths.get(targetUri.getPath());
        } else {
            outFilePath = Paths.get(FileUtil.getFullDocIdPathFromTreeUri(targetUri, getContext()));
        }

        copyFileAttributes(inFilePath, outFilePath);
    }

    public void addThumbsUsingTreeUris(View view) {
        isProcessing = true;
        stopProcessing = false;

        mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.clear();
                updateUiLog(getString(R.string.frag1_log_starting));

                {
                    updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_workingdir_perm), 1));
                    if (!WorkingDirPermActivity.isWorkingDirPermOk(getContext())) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_ko)+"</span><br>", 1));
                        setIsProcessFalse(view);
                        stopProcessing = false;
                        return;
                    }
                    updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_ok)+"</span><br>", 1));
                }

                InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
                Object[] srcDirs;
                if (MainApplication.useSAF) {
                    srcDirs = inputDirs.toUriArray(); // Uri[]
                } else {
                    srcDirs = inputDirs.toFileArray(getContext()); // File[]
                }

                List<UriPermission> persUriPermList = getActivity().getContentResolver().getPersistedUriPermissions();

                // Iterate on folders containing source images
                for (int j = 0; j < srcDirs.length; j++) {
                    if (srcDirs[j] instanceof Uri)
                        updateUiLog(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, FileUtil.getFullPathFromTreeUri((Uri)srcDirs[j], getContext())) + "</b></u><br>",1));
                    else if (srcDirs[j] instanceof File)
                        updateUiLog(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, ((File)srcDirs[j]).toPath()) + "</b></u><br>",1));

                    if (srcDirs[j] instanceof Uri) {
                        // Check permission in case we use SAF...
                        // If we don't have permission, continue to next srcDir
                        updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_perm), 1));
                        boolean perm_ok = false;
                        String tString = srcDirs[j].toString();
                        for (UriPermission perm : persUriPermList) {
                            if (tString.startsWith(perm.getUri().toString())) {
                                if (perm.isReadPermission() && perm.isWritePermission()) {
                                    perm_ok = true;
                                    break;
                                }
                            }
                        }
                        if (!perm_ok) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_not_granted)+"</span><br>", 1));
                            continue;
                        }
                        updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_ok)+"</span><br>", 1));

                    }

                    // 1. build list of files to process
                    ETADocs etaDocs = new ETADocs(getContext(), srcDirs[j]);
                    TreeSet<Object> docs = (TreeSet<Object>)etaDocs.getDocsSet();

                    updateUiLog(Html.fromHtml(getString(R.string.frag1_log_count_files_to_process, docs.size() ) + "<br>",1));

                    // 1. Iterate on all files
                    int i = 0;
                    for (Object _doc : docs) {
                        i++;

                        // Convert (Object)_doc to (Uri)doc or (File)doc
                        ETADoc doc = null;
                        if (srcDirs[j] instanceof Uri) {
                            doc = new ETADocDf((DocumentFile) _doc, getContext(), etaDocs, false);
                        } else if (srcDirs[j] instanceof File) {
                            doc = new ETADocFile((File) _doc, getContext(), etaDocs, true);
                        }
                        if (doc == null) throw new UnsupportedOperationException();

                        if (stopProcessing) {
                            setIsProcessFalse(view);
                            stopProcessing = false;
                            updateUiLog(Html.fromHtml("<br><br>"+getString(R.string.frag1_log_stopped_by_user),1));
                            return;
                        }

                        String subDir = doc.getSubDir();

                        updateUiLog("⋅ [" + i + "/" + docs.size() + "] " +
                                subDir + (subDir.isEmpty() ? "" : File.separator) +
                                doc.getName() + "... ");

                        if (!doc.exists()) {
                            updateUiLog(getString(R.string.frag1_log_skipping_file_missing));
                            continue;
                        }

                        if (!doc.isJpeg()) {
                            updateUiLog(getString(R.string.frag1_log_skipping_not_jpeg));
                            continue;
                        }

                        if (doc.length() == 0) {
                            updateUiLog(getString(R.string.frag1_log_skipping_empty_file));
                            continue;
                        }

                        // a. check if sourceFile already has Exif Thumbnail
                        ExifInterface srcImgExifInterface = null;
                        InputStream srcImgIs = null;
                        ByteArrayOutputStream newImgOs = new ByteArrayOutputStream();

                        boolean srcImgHasThumbnail = false;
                        int srcImgDegrees = 0;

                        try {
                            srcImgIs = doc.inputStream();
                            srcImgExifInterface = new ExifInterface(srcImgIs);
                            if (srcImgExifInterface != null) {
                                srcImgHasThumbnail = srcImgExifInterface.hasThumbnail();
                                srcImgDegrees = srcImgExifInterface.getRotationDegrees();
                            }
                            srcImgIs.close();
                            srcImgExifInterface = null;

                            if (srcImgHasThumbnail && prefs.getBoolean("skipPicsHavingThumbnail", true)) {
                                updateUiLog(getString(R.string.frag1_log_skipping_has_thumbnail));
                                continue;
                            }
                        } catch (Exception e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }

                        Bitmap thumbnail;
                        // a. extract thumbnail & write to output stream
                        try {
                            //if (enableLog) Log.i(TAG, "Creating thumbnail");
                            thumbnail = makeThumbnailRotated(
                                    doc,
                                    prefs.getBoolean("rotateThumbnails", true),
                                    srcImgDegrees);
                            srcImgIs = doc.inputStream();

                            switch (prefs.getString("exif_library", "exiflib_exiv2")) {
                                case "exiflib_android-exif-extended":
                                    writeThumbnailWithAndroidExifExtended(srcImgIs, newImgOs, doc, thumbnail);
                                    break;
                                case "exiflib_pixymeta":
                                    if (!PixymetaInterface.hasPixymetaLib()) {
                                        updateUiLog(Html.fromHtml("<br><br><span style='color:red'>" + getString(R.string.frag1_log_pixymeta_missing) + "</span><br>", 1));
                                        return;
                                    }
                                    PixymetaInterface.writeThumbnailWithPixymeta(srcImgIs, newImgOs, thumbnail);
                                    break;
                            }

                            // Close Streams
                            srcImgIs.close();
                            newImgOs.close();
                        } catch (BadOriginalImageException e) {
                            updateUiLog(getString(R.string.frag1_log_skipping_bad_image));
                            e.printStackTrace();
                            continue;
                        } catch (Exception e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        } catch (AssertionError e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.toString()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }

                        // a. create output dirs
                        doc.createDirForTmp();
                        doc.createDirForBackup();
                        doc.createDirForDest();

                        // a. write outputstream to disk
                        try  {
                            doc.writeInTmp(newImgOs);
                        } catch (Exception e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }

                        if (thumbnail != null) {
                            switch (prefs.getString("exif_library", "exiflib_exiv2")) {
                                case "exiflib_libexif":
                                    try {
                                        String outFilepath = doc.getTmpFSPathWithFilename();

                                        new NativeLibHelper().writeThumbnailWithLibexifThroughFile(
                                                doc.getFullFSPath(),
                                                outFilepath,
                                                thumbnail,
                                                prefs.getBoolean("libexifSkipOnError", true));
                                    } catch (LibexifException e) {
                                        e.printStackTrace();
                                        if (prefs.getBoolean("libexifSkipOnError", true)) {
                                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                            continue;
                                        } else {
                                            updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + e.getMessage() + "</span>", 1));
                                            updateUiLog(Html.fromHtml("<span style='color:blue'>&nbsp;" + getString(R.string.frag1_log_continue_despite_error_as_per_setting) + "</span>", 1));
                                        }
                                    } catch (Exception e) {
                                        updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                        e.printStackTrace();
                                        continue;
                                    }
                                    break;

                                case "exiflib_exiv2":
                                    try {
                                        String outFilepath = doc.getTmpFSPathWithFilename();

                                        // copy original picture to location of tmp picture on which exiv2 will operate.
                                        Uri targetUri = null;
                                        try {
                                            if (doc instanceof ETADocDf) {
                                                targetUri = copyDocument(
                                                        doc.getUri(),
                                                        doc.getTmpUri(),
                                                        true,
                                                        prefs.getBoolean("keepTimeStampOnBackup", false));
                                            } else if (doc instanceof ETADocFile) {
                                                Files.copy(
                                                        doc.toPath(),
                                                        doc.getTmpPath().resolve(doc.getName()),
                                                        REPLACE_EXISTING,
                                                        COPY_ATTRIBUTES);
                                            }
                                        } catch (CopyAttributesFailedException e) {
                                            updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                                            e.printStackTrace();
                                        } catch (Exception e) {
                                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_copying_doc, e.getMessage()) + "</span><br>", 1));
                                            e.printStackTrace();
                                            continue;
                                        }

                                        new NativeLibHelper().writeThumbnailWithExiv2ThroughFile(
                                                outFilepath,
                                                thumbnail,
                                                prefs.getString("exiv2SkipOnLogLevel", "warn"));
                                    } catch (Exiv2ErrorException | Exiv2WarnException e) {
                                        e.printStackTrace();
                                        switch (prefs.getString("exiv2SkipOnLogLevel", "warn")) {
                                            case "warn":
                                            case "error":
                                                updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                                continue;
                                            case "none":
                                                updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + e.getMessage() + "</span>", 1));
                                                updateUiLog(Html.fromHtml("<span style='color:blue'>&nbsp;" + getString(R.string.frag1_log_continue_despite_error_as_per_setting) + "</span>", 1));
                                        }
                                    } catch (Exception e) {
                                        updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                        e.printStackTrace();
                                        continue;
                                    }
                                    break;
                            }
                        }

                        // a. Copy attributes from original file to tmp file
                        try {
                            if (doc instanceof ETADocDf)
                                copyFileAttributes(doc.getUri(), (Uri)doc.getOutputInTmp());
                            else if (doc instanceof ETADocFile)
                                copyFileAttributes(doc.toPath(), ((File)doc.getOutputInTmp()).toPath());
                        } catch (CopyAttributesFailedException e) {
                            updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                        } catch (Exception e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }

                        Uri outputFile = null; // Uri returned by "copyDocument" to dest
                        Uri originalImage = null; // Uri returned by "moveDocument", file in bak.

                        // a. Move or copy original files (from DCIM) to backup dir (DCIM.bak)
                        if (prefs.getBoolean("backupOriginalPic", true)) {
                            if (prefs.getBoolean("writeThumbnailedToOriginalFolder", false)) {
                                // We do a move (so that the file with a thumbnail can be placed to the original dir)
                                try {
                                    if (doc instanceof ETADocDf) {
                                        originalImage = moveDocument(
                                                doc.getUri(),
                                                UriUtil.buildDParentAsUri(doc.getUri()),
                                                doc.getBackupUri(),
                                                false);
                                    } else if (doc instanceof ETADocFile) {
                                        if (etaDocs.getVolumeName() == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                                            try {
                                                Files.move(
                                                        doc.toPath(),
                                                        doc.getBackupPath().resolve(doc.getName()),
                                                        ATOMIC_MOVE);
                                            } catch (Exception e) {
                                                updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_copying_doc, e.getMessage()) + "</span><br>", 1));
                                                e.printStackTrace();
                                                continue;
                                            }
                                        } else {
                                            // Do nothing
                                        }
                                    }
                                } catch (DestinationFileExistsException e) {
                                    updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_cannot_move_to_backup)+"</span><br>",1));
                                    e.printStackTrace();
                                    continue;
                                } catch (CopyAttributesFailedException e) {
                                    updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_moving_doc, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                    continue;
                                }
                            } else {
                                // We do a copy
                                try {
                                    if (doc instanceof ETADocDf) {
                                        copyDocument(
                                                doc.getUri(),
                                                doc.getBackupUri(),
                                                true,
                                                prefs.getBoolean("keepTimeStampOnBackup", true));
                                    } else if (doc instanceof ETADocFile) {
                                        Files.copy(
                                                doc.toPath(),
                                                doc.getBackupPath().resolve(doc.getName()),
                                                REPLACE_EXISTING,
                                                COPY_ATTRIBUTES);
                                    }
                                } catch (CopyAttributesFailedException e) {
                                    updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_copying_doc, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                        }

                        // a. Move new file (having Thumbnail) from tmp folder to its final folder
                        // final folder depends on the setting: "writeThumbnailedToOriginalFolder"
                        boolean replaceExising = false;
                        if ( prefs.getBoolean("overwriteDestPic", false)) {
                            replaceExising = true;
                        }

                        try {
                            if (doc instanceof ETADocDf) {
                                outputFile = moveDocument(
                                        (Uri)doc.getOutputInTmp(),
                                        doc.getTmpUri(),
                                        doc.getDestUri(),
                                        replaceExising);
                            } else if (doc instanceof ETADocFile) {
                                Files.move(
                                        ((File)doc.getOutputInTmp()).toPath(),
                                        doc.getDestPath().resolve(doc.getName()),
                                        REPLACE_EXISTING, ATOMIC_MOVE);
                            }
                        } catch (DestinationFileExistsException e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>"+ getString(R.string.frag1_log_overwrite_not_allowed)+"</span><br>",1));
                            e.printStackTrace();
                            continue;
                        } catch (CopyAttributesFailedException e) {
                            updateUiLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                        } catch (Exception e) {
                            updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_moving_doc, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }

                        updateUiLog(Html.fromHtml("<span style='color:green'>" + getString(R.string.frag1_log_done) + "</span><br>",1));

                        //Update the value background thread to UI thread
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //textViewLog.setText(log);
                            }
                        });
                    }
                }
                updateUiLog(getString(R.string.frag1_log_finished));

                setIsProcessFalse(view);
            }
        }).start();
    }

    private Uri copyDocument(Uri sourceUri, Uri targetParentUri, boolean replaceExisting, boolean copyFileAttributes)
        throws Exception {
        // INFO : copy looses timestamp, so we copyAttributes at the end.
        String displayName, targetParentPath;

        if (sourceUri.getScheme().equals("file")) {
            displayName = sourceUri.getLastPathSegment();
        } else {
            displayName = UriUtil.getDName(sourceUri);
        }

        Uri targetUri = null, targetTmpUri = null;
        boolean targetExists = false, targetTmpExists = false;
        File targetFile = null, targetTmpFile = null;
        if (targetParentUri.getScheme().equals("file")) {
            targetParentPath = targetParentUri.getPath();
            targetFile = new File(targetParentPath + File.separator + displayName);
            targetUri = Uri.fromFile(targetFile);
            targetExists = targetFile.exists();
            targetTmpFile = new File(targetParentPath + File.separator + displayName + "_tmp");
            targetTmpUri = Uri.fromFile(targetTmpFile);
            targetTmpExists = targetTmpFile.exists();
        } else {
            targetParentPath = UriUtil.getDocId(targetParentUri);

            targetUri = DocumentsContract.buildDocumentUriUsingTree(
                    targetParentUri,
                    targetParentPath + File.separator + displayName );
            targetExists = DocumentFile.fromTreeUri(getContext(), targetUri).exists();

            targetTmpUri = DocumentsContract.buildDocumentUriUsingTree(
                    targetParentUri,
                    targetParentPath + File.separator + displayName + "_tmp" );
            targetTmpExists = DocumentFile.fromTreeUri(getContext(), targetTmpUri).exists();
        }

        if (targetExists && !replaceExisting) {
            if (enableLog) Log.i(TAG, getString(R.string.frag1_log_file_exists, targetUri.toString()));
            return null;
        }

        if (!targetTmpExists) {
            try {
                //targetUri = DocumentsContract.createDocument(getActivity().getContentResolver(), targetParentUri, "image/jpeg", displayName);
                // We don't use the correct filename but another one in the hope that this will
                // avoid indexing the file while its attributes are not fully copied. hence we don't set mimeType and use
                // a temporary extension
                if (targetParentUri.getScheme().equals("file")) {
                    targetTmpFile.createNewFile();
                } else {
                    targetTmpUri = DocumentsContract.createDocument(getActivity().getContentResolver(), targetParentUri, "", displayName + "_tmp");
                }
            } catch (Exception e) {
                throw e;
                //e.printStackTrace();
            }
        }

        copyDocument(sourceUri, targetParentUri, targetTmpUri);

        try {
            // Copy attributes of source file to the target file
            if (copyFileAttributes)
                copyFileAttributes(sourceUri, targetTmpUri);
        } catch (CopyAttributesFailedException e) {
            throw e;
        } finally {
            // We don't use the correct filename but another one in the hope that this will
            // avoid indexing the file while its attributes are not fully copied. Hence, here we rename
            // the file to its correct name.
            if (targetExists) {
                try {
                    if (targetParentUri.getScheme().equals("file")) {
                        targetFile.delete();
                    } else {
                        DocumentsContract.deleteDocument(getActivity().getContentResolver(), targetUri);
                    }
                } catch (Exception e) {
                    throw e;
                    //e.printStackTrace();
                }
            }
            if (targetParentUri.getScheme().equals("file")) {
                targetTmpFile.renameTo(targetFile);
            } else {
                DocumentsContract.renameDocument(getActivity().getContentResolver(), targetTmpUri, displayName);
            }
        }
        return targetUri;
    }

    private void copyDocument(Uri sourceUri, Uri targetParentUri, Uri targetUri) throws Exception {
        // Copy is most time not possible. See:
        // https://stackoverflow.com/questions/66660155/android-saf-cannot-copy-file-flag-supports-copy-not-set
        // So we check if it is supported, otherwise, we fall back to copying with streams.

        if (UriUtil.supportsCopy(getContext(), sourceUri)) {
            // Delete destnation file
            // TODO I don't know if this is actually necessary. We'll see if the case happens

            if (enableLog) Log.i(TAG, "File supports DocumentsContract.copyDocument... Copying...");
            try {
                Uri outUri = DocumentsContract.copyDocument(getActivity().getContentResolver(), sourceUri, targetParentUri);
            } catch (Exception e ) {
                throw e;
                //e.printStackTrace();
            }
        } else {
            copyDocumentWithStream(sourceUri, targetUri);
        }
    }

    private void copyDocumentWithStream(Uri sourceUri, Uri targetUri) throws Exception {
        try {
            InputStream is = getActivity().getContentResolver().openInputStream(sourceUri);
            OutputStream os = getActivity().getContentResolver().openOutputStream(targetUri);

            byte[] buf = new byte[8192];
            int length;
            while ((length = is.read(buf)) > 0) {
                os.write(buf, 0, length);
            }
            os.close();
            is.close();
        } catch (Exception e) {
            throw e;
            //e.printStackTrace();
        }
    }

    private Uri moveDocument(Uri sourceUri, Uri sourceParentUri, Uri targetParentUri, boolean replaceExisting)
            throws DestinationFileExistsException, Exception {
        // INFO : move keeps timestamp
        String displayName, targetParentPath;

        if (sourceUri.getScheme().equals("file")) {
            displayName = sourceUri.getLastPathSegment();
        } else {
            displayName = UriUtil.getDName(sourceUri);
        }

        targetParentPath = UriUtil.getDocId(targetParentUri);

        Uri targetUri = DocumentsContract.buildDocumentUriUsingTree(
                targetParentUri,
                targetParentPath + File.separator + displayName);

        boolean targetExists = DocumentFile.fromTreeUri(getContext(), targetUri).exists();

        if (targetExists && !replaceExisting) {
            if (enableLog) Log.i(TAG, getString(R.string.frag1_log_file_exists, targetUri.toString()));
            throw new DestinationFileExistsException();
        }

        try {

            if (targetExists) {
                DocumentsContract.deleteDocument(getActivity().getContentResolver(), targetUri);
            }

            if (sourceUri.getScheme().equals("file")) {
                copyDocument(sourceUri, targetParentUri,true, true);
                new File(sourceUri.getPath()).delete();
                return targetUri;
            } else {
                return DocumentsContract.moveDocument(getActivity().getContentResolver(), sourceUri, sourceParentUri, targetParentUri);
            }

        } catch (Exception e) {
            throw e;
            //e.printStackTrace();
        }
    }

    private void writeThumbnailWithAndroidExifExtended (
            InputStream srcImgIs, OutputStream newImgOs, ETADoc doc, Bitmap thumbnail)
            throws Exception, AssertionError {
        try {
            // Andoid-Exif-Extended will write twice the APP1 structure to the file,
            // but it seems to copy perfectly

            // It was necessary to add the method writeExif(InputStream, OutputStream)
            // see https://github.com/sephiroth74/Android-Exif-Extended/pull/24

            it.sephiroth.android.library.exif2.ExifInterface sInExif = new it.sephiroth.android.library.exif2.ExifInterface();

            sInExif.readExif(srcImgIs, it.sephiroth.android.library.exif2.ExifInterface.Options.OPTION_ALL );
            sInExif.setCompressedThumbnail(thumbnail);

            //set other mandatory tags for IFD1 (compression, resolution, res unit)
            sInExif.setTag(sInExif.buildTag(it.sephiroth.android.library.exif2.ExifInterface.TAG_COMPRESSION,IfdId.TYPE_IFD_1, it.sephiroth.android.library.exif2.ExifInterface.Compression.JPEG));
            sInExif.setTag(sInExif.buildTag(it.sephiroth.android.library.exif2.ExifInterface.TAG_RESOLUTION_UNIT,IfdId.TYPE_IFD_1, it.sephiroth.android.library.exif2.ExifInterface.ResolutionUnit.INCHES));
            sInExif.setTag(sInExif.buildTag(it.sephiroth.android.library.exif2.ExifInterface.TAG_X_RESOLUTION,IfdId.TYPE_IFD_1, new Rational(72,1)));
            sInExif.setTag(sInExif.buildTag(it.sephiroth.android.library.exif2.ExifInterface.TAG_Y_RESOLUTION,IfdId.TYPE_IFD_1, new Rational(72,1)));

            // Close & Reopen InputStream, otherwise writeExif will fail with an exception
            // because srcImgIs was already read
            srcImgIs.close();
            srcImgIs = doc.inputStream();

            // writeExif recopies anyway the tags that are in srcImgIs (which will be added
            // to those already in sInExif). It is necessary to call readExif,
            // otherwise addition of tags will crash (internal_writer needs a base coming
            // from another file, ie. for the SOS tag
            sInExif.writeExif(srcImgIs, newImgOs);
        } catch (Exception e) {
            throw e;
        }
    }

    public void showSettingsActivity(View view) {
        Intent intent = new Intent(getContext(), SettingsActivity.class);
        startActivity(intent);
    }

    public static void updateTextViewDirList(Context ctx, TextView textView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
        if (inputDirs.size() == 0) {
            textView.setText(R.string.frag1_text_no_dir_selected);
        } else {
            textView.setText(inputDirs.toStringForDisplay(ctx));
        }
    }

    public void updateUiLog(String text) {
        if (enableLog) Log.i(TAG, text);
        log.append(text);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLog.setText(log);
                // Stuff that updates the UI
                scrollview.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });

    }
    public void updateUiLog(Spanned text) {
        if (enableLog) Log.i(TAG, text.toString());
        log.append(text);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLog.setText(log);
                // Stuff that updates the UI
                scrollview.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    public void setIsProcessFalse(View view) {
        isProcessing = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button start = (Button)getView().findViewById(R.id.button_addThumbs);
                Button stop =  (Button)getView().findViewById(R.id.button_stopProcess);
                start.setVisibility(Button.VISIBLE);
                stop.setVisibility(Button.GONE);
            }
        });
    }

    public static byte[] bitmapToJPEGBytearray(Bitmap thumbnail) {
        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapdata = bos.toByteArray();
        return bitmapdata;
    }

    public static class BadOriginalImageException extends Exception {}
    public static class DestinationFileExistsException extends Exception {}
    public static class CopyAttributesFailedException extends Exception {
        public CopyAttributesFailedException(Throwable err) {
            super(err);
        }
    }

}
