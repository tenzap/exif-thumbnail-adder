package com.exifthumbnailadder.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.exifthumbnailadder.app.exception.BadOriginalImageException;
import com.exifthumbnailadder.app.exception.CopyAttributesFailedException;
import com.exifthumbnailadder.app.exception.DestinationFileExistsException;
import com.exifthumbnailadder.app.exception.Exiv2ErrorException;
import com.exifthumbnailadder.app.exception.Exiv2WarnException;
import com.exifthumbnailadder.app.exception.LibexifException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.TreeSet;

import it.sephiroth.android.library.exif2.IfdId;
import it.sephiroth.android.library.exif2.Rational;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class AddThumbsService extends Service {
    private final static int NOTIFICATION_ID = 99999;
    private final static String CHANNEL_ID = "1";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private LocalBroadcastManager broadcaster;
    private boolean stopProcessing = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            doProcessing();

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    public AddThumbsService() {
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Notification ID cannot be 0.
        startForeground(NOTIFICATION_ID, getMyActivityNotification(""));

        LastServiceLiveData.get().setLastService(this.getClass().getName());

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, don't restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        stopProcessing = true;
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void updateLogAndNotif(String message) {
        updateNotification(message);
        updateLog(message);
    }

    private void updateLogAndNotif(Spanned message) {
        updateNotification(message.toString());
        updateLog(message);
    }

    private void updateLog(String message) {
        AddThumbsLogLiveData.get().appendLog(message);
    }

    private void updateLog(Spanned message) {
        AddThumbsLogLiveData.get().appendLog(message);
    }

    private void sendFinished() {
        Intent intent = new Intent("com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_FINISHED");
        broadcaster.sendBroadcast(intent);
    }

    // https://stackoverflow.com/questions/5528288/how-do-i-update-the-notification-text-for-a-foreground-service-in-android
    private Notification getMyActivityNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getText(R.string.frag1_text_processing_log);
            String description = getText(R.string.frag1_text_processing_log).toString();
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Don't see these lines in your code...
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification =
                new Notification.Builder(this , CHANNEL_ID)
                        .setContentTitle(getString(R.string.frag1_log_processing_dir, "'" + getString(R.string.action_add_thumbs) + "'"))
                        .setContentText(text)
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.ic_notif_status_bar)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.frag1_text_processing_log))
                        .build();

        return notification;
    }

    /**
     * This is the method that can be called to update the Notification
     */
    private void updateNotification(String text){
        if (ServiceUtil.isServiceRunning(getApplicationContext(), AddThumbsService.class)) {
            Notification notification = getMyActivityNotification(text);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void doProcessing() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
        Object[] srcDirs;
        if (prefs.getBoolean("useSAF", true)) {
            srcDirs = inputDirs.toUriArray(); // Uri[]
        } else {
            srcDirs = inputDirs.toFileArray(getApplicationContext()); // File[]
        }

        // Iterate on folders containing source images
        for (int j = 0; j < srcDirs.length; j++) {
            ETASrcDir etaSrcDir = null;
            if (srcDirs[j] instanceof Uri) {
                etaSrcDir = new ETASrcDirUri(getApplicationContext(), (Uri)srcDirs[j]);
            } else if (srcDirs[j] instanceof File) {
                etaSrcDir = new ETASrcDirFile(getApplicationContext(), (File)srcDirs[j]);
            }

            updateLogAndNotif(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, etaSrcDir.getFSPath()) + "</b></u><br>",1));

            // Check permission in case we use SAF...
            // If we don't have permission, continue to next srcDir
            updateLogAndNotif(Html.fromHtml(getString(R.string.frag1_log_checking_perm), 1));
            if (! etaSrcDir.isPermOk()) {
                updateLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_not_granted)+"</span><br>", 1));
                continue;
            }
            updateLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_successful)+"</span><br>", 1));

            // 1. build list of files to process
            @SuppressWarnings("unchecked")
            TreeSet<Object> docs = (TreeSet<Object>) etaSrcDir.getDocsSet();
            updateLogAndNotif(Html.fromHtml(getString(R.string.frag1_log_count_files_to_process, docs.size() ) + "<br>",1));

            // 1. Iterate on all files
            int i = 0;
            for (Object _doc : docs) {
                i++;

                // Convert (Object)_doc to (Uri)doc or (File)doc
                ETADoc doc = null;
                if (etaSrcDir instanceof ETASrcDirUri) {
                    doc = new ETADocDf((DocumentFile) _doc, getApplicationContext(), (ETASrcDirUri)etaSrcDir, false);
                } else if (etaSrcDir instanceof ETASrcDirFile) {
                    doc = new ETADocFile((File) _doc, getApplicationContext(), (ETASrcDirFile)etaSrcDir, true);
                }
                if (doc == null) throw new UnsupportedOperationException();

                if (stopProcessing) {
                    stopProcessing = false;
                    updateLogAndNotif(Html.fromHtml("<br><br>"+getString(R.string.frag1_log_stopped_by_user),1));
                    stopSelf();
                    return;
                }

                String subDir = doc.getSubDir();

                updateLogAndNotif("⋅ [" + i + "/" + docs.size() + "] " +
                        subDir + (subDir.isEmpty() ? "" : File.separator) +
                        doc.getName() + "... ");

                if (!doc.exists()) {
                    updateLog(getString(R.string.frag1_log_skipping_file_missing));
                    continue;
                }

                if (!doc.isJpeg()) {
                    updateLog(getString(R.string.frag1_log_skipping_not_jpeg));
                    continue;
                }

                if (doc.length() == 0) {
                    updateLog(getString(R.string.frag1_log_skipping_empty_file));
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
                        updateLog(getString(R.string.frag1_log_skipping_has_thumbnail));
                        continue;
                    }
                } catch (Exception e) {
                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                    e.printStackTrace();
                    continue;
                }

                Bitmap thumbnail;
                // a. extract thumbnail & write to output stream
                try {
                    //if (enableLog) Log.i(TAG, "Creating thumbnail");
                    thumbnail = doc.getThumbnail(
                            "ffmpeg",
                            prefs.getBoolean("rotateThumbnails", true),
                            srcImgDegrees);
                    srcImgIs = doc.inputStream();

                    switch (prefs.getString("exif_library", "exiflib_exiv2")) {
                        case "exiflib_android-exif-extended":
                            writeThumbnailWithAndroidExifExtended(srcImgIs, newImgOs, doc, thumbnail);
                            break;
                        case "exiflib_pixymeta":
                            if (!PixymetaInterface.hasPixymetaLib()) {
                                updateLog(Html.fromHtml("<br><br><span style='color:red'>" + getString(R.string.frag1_log_pixymeta_missing) + "</span><br>", 1));
                                sendFinished();
                                stopSelf();
                                return;
                            }
                            PixymetaInterface.writeThumbnailWithPixymeta(srcImgIs, newImgOs, thumbnail);
                            break;
                    }

                    // Close Streams
                    srcImgIs.close();
                    newImgOs.close();
                } catch (BadOriginalImageException e) {
                    updateLog(getString(R.string.frag1_log_skipping_bad_image));
                    e.printStackTrace();
                    continue;
                } catch (Exception e) {
                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                    e.printStackTrace();
                    continue;
                } catch (AssertionError e) {
                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.toString()) + "</span><br>", 1));
                    e.printStackTrace();
                    continue;
                }

                // a. create output dirs
                doc.createDirForTmp();
                doc.createDirForBackup();
                doc.createDirForDest();

                try {
                    doc.storeFileAttributes();
                } catch (Exception e) {
                    updateLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_store_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                    e.printStackTrace();
                }

                // a. write outputstream to disk
                try  {
                    doc.writeInTmp(newImgOs);
                } catch (Exception e) {
                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
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
                                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                    continue;
                                } else {
                                    updateLog(Html.fromHtml("<span style='color:#FFA500'>" + e.getMessage() + "</span>", 1));
                                    updateLog(Html.fromHtml("<span style='color:blue'>&nbsp;" + getString(R.string.frag1_log_continue_despite_error_as_per_setting) + "</span>", 1));
                                }
                            } catch (Exception e) {
                                updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
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
                                                false);
                                    } else if (doc instanceof ETADocFile) {
                                        Files.copy(
                                                doc.toPath(),
                                                doc.getTmpPath().resolve(doc.getName()),
                                                REPLACE_EXISTING);
                                    }
                                } catch (CopyAttributesFailedException e) {
                                    updateLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_copying_doc, e.getMessage()) + "</span><br>", 1));
                                    e.printStackTrace();
                                    continue;
                                }

                                new NativeLibHelper().writeThumbnailWithExiv2ThroughFile(
                                        outFilepath,
                                        thumbnail,
                                        prefs.getString("exiv2SkipOnLogLevel", "warn"));
                            } catch (Exiv2WarnException e) {
                                e.printStackTrace();
                                switch (prefs.getString("exiv2SkipOnLogLevel", "warn")) {
                                    case "warn":
                                        if (doc instanceof ETADocFile) { doc.deleteOutputInTmp(); }
                                        updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                        continue;
                                    case "error":
                                    case "none":
                                        updateLog(Html.fromHtml("<span style='color:#FFA500'>" + e.getMessage() + "</span>", 1));
                                        updateLog(Html.fromHtml("<span style='color:blue'>&nbsp;" + getString(R.string.frag1_log_continue_despite_error_as_per_setting) + "</span>", 1));
                                }
                            } catch (Exiv2ErrorException e) {
                                e.printStackTrace();
                                switch (prefs.getString("exiv2SkipOnLogLevel", "warn")) {
                                    case "warn":
                                    case "error":
                                        if (doc instanceof ETADocFile) { doc.deleteOutputInTmp(); }
                                        updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                        continue;
                                    case "none":
                                        updateLog(Html.fromHtml("<span style='color:#FFA500'>" + e.getMessage() + "</span>", 1));
                                        updateLog(Html.fromHtml("<span style='color:blue'>&nbsp;" + getString(R.string.frag1_log_continue_despite_error_as_per_setting) + "</span>", 1));
                                }
                            } catch (Exception e) {
                                updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.getMessage()) + "</span><br>", 1));
                                e.printStackTrace();
                                continue;
                            }
                            break;
                    }
                }

                // a. We don't copy attributes from original file to tmp file because from Android 11
                // it may fail on setOwner when tmp file is put in Cache Dir
                // We set the attributes on the output files another way

                Uri outputUri = null; // Uri returned by "copyDocument" to dest
                Uri originalImageUri = null; // Uri returned by "moveDocument", file in bak.
                Path outputPath = null;
                Path originalImagePath = null;

                // a. Move or copy original files (from DCIM) to backup dir (DCIM.bak)
                if (prefs.getBoolean("backupOriginalPic", true)) {
                    if (prefs.getBoolean("writeThumbnailedToOriginalFolder", false)) {
                        // We do a move (so that the file with a thumbnail can be placed to the original dir)
                        try {
                            if (doc instanceof ETADocDf) {
                                originalImageUri = moveDocument(
                                        doc.getUri(),
                                        UriUtil.buildDParentAsUri(doc.getUri()),
                                        doc.getBackupUri(),
                                        false);
                            } else if (doc instanceof ETADocFile) {
                                if (etaSrcDir.getVolumeName() == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                                    Path backupFile = doc.getBackupPath().resolve(doc.getName());
                                    if (backupFile.toFile().exists())
                                        throw new FileAlreadyExistsException(backupFile.toString());
                                    try {
                                        originalImagePath = Files.move(
                                                doc.toPath(),
                                                backupFile,
                                                ATOMIC_MOVE);
                                    } catch (AtomicMoveNotSupportedException e) {
                                        if (enableLog) Log.i(TAG, "Error moving document. Trying 'move' without ATOMIC_MOVE option. " + e.getMessage());
                                        originalImagePath = Files.move(
                                                doc.toPath(),
                                                backupFile);
                                        doc.copyAttributesTo(originalImagePath);
                                    }
                                } else {
                                    // Do nothing
                                }
                            }
                        } catch (DestinationFileExistsException | FileAlreadyExistsException e) {
                            updateLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_cannot_move_to_backup)+"</span><br>",1));
                            e.printStackTrace();
                            continue;
                        } catch (CopyAttributesFailedException e) {
                            updateLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                        } catch (Exception e) {
                            updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_moving_doc, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        // We do a copy
                        try {
                            if (doc instanceof ETADocDf) {
                                originalImageUri = copyDocument(
                                        doc.getUri(),
                                        doc.getBackupUri(),
                                        true,
                                        false);
                                if (prefs.getBoolean("keepTimeStampOnBackup", true))
                                    doc.copyAttributesTo(originalImageUri);
                            } else if (doc instanceof ETADocFile) {
                                if (prefs.getBoolean("keepTimeStampOnBackup", true)) {
                                    originalImagePath = Files.copy(
                                            doc.toPath(),
                                            doc.getBackupPath().resolve(doc.getName()),
                                            REPLACE_EXISTING,
                                            COPY_ATTRIBUTES);
                                } else {
                                    originalImagePath = Files.copy(
                                            doc.toPath(),
                                            doc.getBackupPath().resolve(doc.getName()),
                                            REPLACE_EXISTING);
                                }
                            }
                        } catch (CopyAttributesFailedException e) {
                            updateLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                            e.printStackTrace();
                        } catch (Exception e) {
                            updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_copying_doc, e.getMessage()) + "</span><br>", 1));
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
                        outputUri = moveDocument(
                                (Uri)doc.getOutputInTmp(),
                                doc.getTmpUri(),
                                doc.getDestUri(),
                                replaceExising);
                        doc.copyAttributesTo(outputUri);
                    } else if (doc instanceof ETADocFile) {
                        Path destFile = doc.getDestPath().resolve(doc.getName());
                        if (replaceExising) {
                            try {
                                outputPath = Files.move(
                                        ((File) doc.getOutputInTmp()).toPath(),
                                        destFile,
                                        REPLACE_EXISTING, ATOMIC_MOVE);
                            } catch (AtomicMoveNotSupportedException e) {
                                if (enableLog) Log.i(TAG, "Error moving document. Trying 'move' without ATOMIC_MOVE option. " + e.getMessage());
                                outputPath = Files.move(
                                        ((File) doc.getOutputInTmp()).toPath(),
                                        destFile,
                                        REPLACE_EXISTING);
                            }
                        } else {
                            if (destFile.toFile().exists())
                                throw new FileAlreadyExistsException(destFile.toString());
                            try {
                                outputPath = Files.move(
                                        ((File) doc.getOutputInTmp()).toPath(),
                                        destFile,
                                        ATOMIC_MOVE);
                            } catch (AtomicMoveNotSupportedException e) {
                                if (enableLog) Log.i(TAG, "Error moving document. Trying 'move' without ATOMIC_MOVE option. " + e.getMessage());
                                outputPath = Files.move(
                                        ((File) doc.getOutputInTmp()).toPath(),
                                        destFile);
                            }
                        }
                        doc.copyAttributesTo(outputPath);
                    }
                } catch (DestinationFileExistsException | FileAlreadyExistsException e ) {
                    updateLog(Html.fromHtml("<span style='color:red'>"+ getString(R.string.frag1_log_overwrite_not_allowed)+"</span><br>",1));
                    doc.deleteOutputInTmp();
                    e.printStackTrace();
                    continue;
                } catch (CopyAttributesFailedException e) {
                    updateLog(Html.fromHtml("<span style='color:#FFA500'>" + getString(R.string.frag1_log_could_not_copy_timestamp_and_attr, e.getMessage()) + "</span><br>", 1));
                    e.printStackTrace();
                } catch (Exception e) {
                    updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_error_moving_doc, e.getMessage()) + "</span><br>", 1));
                    e.printStackTrace();
                    continue;
                }

                updateLog(Html.fromHtml("<span style='color:green'>" + getString(R.string.frag1_log_done) + "</span><br>",1));
            }
        }

        updateLogAndNotif(getString(R.string.frag1_log_finished));
        sendFinished();
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
            targetExists = DocumentFile.fromTreeUri(this, targetUri).exists();

            targetTmpUri = DocumentsContract.buildDocumentUriUsingTree(
                    targetParentUri,
                    targetParentPath + File.separator + displayName + "_tmp" );
            targetTmpExists = DocumentFile.fromTreeUri(this, targetTmpUri).exists();
        }

        if (targetExists && !replaceExisting) {
            if (enableLog) Log.i(TAG, getString(R.string.frag1_log_file_exists, targetUri.toString()));
            return null;
        }

        if (!targetTmpExists) {
            try {
                //targetUri = DocumentsContract.createDocument(getContentResolver(), targetParentUri, "image/jpeg", displayName);
                // We don't use the correct filename but another one in the hope that this will
                // avoid indexing the file while its attributes are not fully copied. hence we don't set mimeType and use
                // a temporary extension
                if (targetParentUri.getScheme().equals("file")) {
                    targetTmpFile.createNewFile();
                } else {
                    targetTmpUri = DocumentsContract.createDocument(getContentResolver(), targetParentUri, "", displayName + "_tmp");
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
                        DocumentsContract.deleteDocument(getContentResolver(), targetUri);
                    }
                } catch (Exception e) {
                    throw e;
                    //e.printStackTrace();
                }
            }
            if (targetParentUri.getScheme().equals("file")) {
                targetTmpFile.renameTo(targetFile);
            } else {
                try {
                    DocumentsContract.renameDocument(getContentResolver(), targetTmpUri, displayName);
                } catch (FileNotFoundException e) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                        // On Android P (API 28), there is a bug with renameDocument.
                        // It throws a FileNotFoundException although the document is renamed.
                        // This affects only API 28 and was fixed in API 29
                        // Details here: https://issuetracker.google.com/issues/171286865
                        // We silently skip this exception here.

                        // Do nothing.
                    } else {
                        throw e;
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
        }
        return targetUri;
    }

    private Uri copyDocument(Uri sourceUri, Uri targetParentUri, Uri targetUri) throws Exception {
        // Copy is most time not possible. See:
        // https://stackoverflow.com/questions/66660155/android-saf-cannot-copy-file-flag-supports-copy-not-set
        // So we check if it is supported, otherwise, we fall back to copying with streams.

        if (UriUtil.supportsCopy(this, sourceUri)) {
            // Delete destnation file
            // TODO I don't know if this is actually necessary. We'll see if the case happens

            if (enableLog) Log.i(TAG, "File supports DocumentsContract.copyDocument... Copying...");
            try {
                Uri outUri = DocumentsContract.copyDocument(getContentResolver(), sourceUri, targetParentUri);
                return outUri;
            } catch (Exception e ) {
                throw e;
                //e.printStackTrace();
            }
        } else {
            return copyDocumentWithStream(sourceUri, targetUri);
        }
    }

    private Uri copyDocumentWithStream(Uri sourceUri, Uri targetUri) throws Exception {
        try {
            InputStream is = getContentResolver().openInputStream(sourceUri);
            OutputStream os = getContentResolver().openOutputStream(targetUri);

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
        return targetUri;
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

        boolean targetExists = DocumentFile.fromTreeUri(this, targetUri).exists();

        if (targetExists && !replaceExisting) {
            if (enableLog) Log.i(TAG, getString(R.string.frag1_log_file_exists, targetUri.toString()));
            throw new DestinationFileExistsException();
        }

        try {

            if (targetExists) {
                DocumentsContract.deleteDocument(getContentResolver(), targetUri);
            }

            if (sourceUri.getScheme().equals("file")) {
                copyDocument(sourceUri, targetParentUri,true, true);
                new File(sourceUri.getPath()).delete();
                return targetUri;
            } else {
                return DocumentsContract.moveDocument(getContentResolver(), sourceUri, sourceParentUri, targetParentUri);
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
            sInExif.setTag(sInExif.buildTag(it.sephiroth.android.library.exif2.ExifInterface.TAG_COMPRESSION, IfdId.TYPE_IFD_1, it.sephiroth.android.library.exif2.ExifInterface.Compression.JPEG));
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
            inFilePath = Paths.get(FileUtil.getFullDocIdPathFromTreeUri(sourceUri, this));
        }

        if (targetUri.getScheme().equals("file")) {
            outFilePath = Paths.get(targetUri.getPath());
        } else {
            outFilePath = Paths.get(FileUtil.getFullDocIdPathFromTreeUri(targetUri, this));
        }

        copyFileAttributes(inFilePath, outFilePath);
    }
}
