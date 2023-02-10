package com.exifthumbnailadder.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.TreeSet;

public class SyncService extends Service {
    private final static int NOTIFICATION_ID = 99999;
    private final static String CHANNEL_ID = "1";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private LocalBroadcastManager broadcaster;
    private boolean stopProcessing = false;

    private boolean dryRun;

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

    public SyncService() {
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
        serviceHandler = new SyncService.ServiceHandler(serviceLooper);

        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dryRun = intent.getBooleanExtra("dryRun", true);

        // Notification ID cannot be 0.
        if (Build.VERSION.SDK_INT <= 28) {
            startForeground(NOTIFICATION_ID, getMyActivityNotification(""));
        } else {
            startForeground(NOTIFICATION_ID, getMyActivityNotification(""), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }

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
        SyncLogLiveData.get().appendLog(message);
    }

    private void updateLog(Spanned message) {
        SyncLogLiveData.get().appendLog(message);
    }

    private void sendFinished() {
        Intent intent = new Intent("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED");
        broadcaster.sendBroadcast(intent);
    }

    private void sendStoppedByUser() {
        Intent intent = new Intent("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_STOPPED_BY_USER");
        broadcaster.sendBroadcast(intent);
    }

    // https://stackoverflow.com/questions/5528288/how-do-i-update-the-notification-text-for-a-foreground-service-in-android
    private Notification getMyActivityNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

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
                        .setContentTitle(getString(R.string.frag1_log_processing_dir, "'" + getString(R.string.action_sync) + "'"))
                        .setContentText(text)
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.ic_notif_status_bar)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.frag1_text_processing_log))
                        .setOngoing(true)
                        .build();

        return notification;
    }

    /**
     * This is the method that can be called to update the Notification
     */
    private void updateNotification(String text){
        if (ServiceUtil.isServiceRunning(getApplicationContext(), SyncService.class)) {
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
            if (etaSrcDir == null) throw new UnsupportedOperationException();

            updateLogAndNotif(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, etaSrcDir.getFSPath()) + "</b></u><br>",1));

            // Check permission in case we use SAF...
            // If we don't have permission, continue to next srcDir
            updateLogAndNotif(Html.fromHtml(getString(R.string.frag1_log_checking_perm), 1));
            if (! etaSrcDir.isPermOk()) {
                updateLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_not_granted)+"</span><br>", 1));
                continue;
            }
            updateLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_successful)+"</span><br>", 1));


            ETADoc etaDocSrc = null;
            if (etaSrcDir instanceof ETASrcDirUri) {
                DocumentFile baseDf = DocumentFile.fromTreeUri(getApplicationContext(), (Uri)srcDirs[j]);
                etaDocSrc = new ETADocDf(baseDf, getApplicationContext(), (ETASrcDirUri)etaSrcDir, false);
            } else if (etaSrcDir instanceof ETASrcDirFile) {
                etaDocSrc = new ETADocFile((File)srcDirs[j], getApplicationContext(), (ETASrcDirFile)etaSrcDir, true);
            }
            if (etaDocSrc == null) throw new UnsupportedOperationException();

            // Process backupUri
            ETASrcDir etaSrcDirBackup = null;
            if (etaDocSrc instanceof ETADocDf) {
                etaSrcDirBackup = new ETASrcDirUri(
                        getApplicationContext(),
                        etaDocSrc.getBackupUri());
            } else if (etaDocSrc instanceof ETADocFile) {
                etaSrcDirBackup = new ETASrcDirFile(
                        getApplicationContext(),
                        etaDocSrc.getBackupPath().toFile());
            }
            boolean finished = doSyncForUri(etaSrcDirBackup, etaDocSrc, dryRun);
            if (!finished) {
                stopSelf();
                return;
            }

            // Process outputUri
            if (!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("writeThumbnailedToOriginalFolder", false)) {
                updateLog(Html.fromHtml("<br>",1));
                ETASrcDir etaSrcDirDest = null;
                if (etaDocSrc instanceof ETADocDf) {
                    etaSrcDirDest = new ETASrcDirUri(
                            getApplicationContext(),
                            etaDocSrc.getDestUri());
                } else if (etaDocSrc instanceof ETADocFile) {
                    etaSrcDirDest = new ETASrcDirFile(
                            getApplicationContext(),
                            etaDocSrc.getDestPath().toFile());
                }
                finished = doSyncForUri(etaSrcDirDest, etaDocSrc, dryRun);
                if (!finished) {
                    stopSelf();
                    return;
                }
            }
        }

        updateLogAndNotif(getString(R.string.frag1_log_finished));
        sendFinished();
    }


    private boolean doSyncForUri(ETASrcDir workingDirDocs, ETADoc srcDirEtaDoc, boolean dryRun ) {

        @SuppressWarnings("unchecked")
        TreeSet<Object> docsInWorkingDir = (TreeSet<Object>)workingDirDocs.getDocsSet();

        updateLogAndNotif(getString(R.string.sync_log_checking, workingDirDocs.getFSPathWithoutRoot()));
        updateLog("\n");
        updateLog(Html.fromHtml("<u>" + getString(R.string.sync_log_files_to_remove) + "</u><br>",1));

        for (Object _doc : docsInWorkingDir) {

            // Convert (Object)_doc to (Uri)doc or (File)doc
            ETADoc doc = null;
            if (workingDirDocs.getDocsRoot() instanceof Uri) {
                doc = new ETADocDf((DocumentFile) _doc, getApplicationContext(), (ETASrcDirUri) workingDirDocs, false);
            } else if (workingDirDocs.getDocsRoot() instanceof File) {
                doc = new ETADocFile((File) _doc, getApplicationContext(), (ETASrcDirFile)workingDirDocs, true);
            }
            if (doc == null) throw new UnsupportedOperationException();

            if (stopProcessing) {
                stopProcessing = false;
                updateLogAndNotif(Html.fromHtml("<br><br>"+getString(R.string.frag1_log_stopped_by_user),1));
                sendStoppedByUser();
                return false;
            }

            // Skip some files
            if (doc.isFile()) {
                if (!doc.isJpeg()) continue;
                if (doc.getName().equals(".nomedia")) continue;
            }
            if (doc.isDirectory()) continue;

            boolean srcFileExists = true;
            try {
                if (workingDirDocs.getDocsRoot() instanceof Uri) {
                    Uri srcUri = doc.getSrcUri(srcDirEtaDoc.getMainDir(), srcDirEtaDoc.getTreeId());
                    srcFileExists = DocumentFile.fromTreeUri(getApplicationContext(), srcUri).exists();
                } else if (workingDirDocs.getDocsRoot() instanceof File) {
                    File srcFile = doc.getSrcPath(srcDirEtaDoc.getFullFSPath()).toFile();
                    srcFileExists = srcFile.exists();
                }
            } catch (Exception e) {
                updateLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.toString()) + "</span><br>", 1));
                e.printStackTrace();
                continue;
            }

            // Delete file if it doesn't exist in source directory
            if (!srcFileExists) {
                updateLogAndNotif("⋅ " + doc.getDPath() + "... ");
                if (!dryRun) {
                    boolean deleted = doc.delete();
                    if (deleted)
                        updateLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_done)+"</span>",1));
                    else
                        updateLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.sync_log_failure_to_delete_file)+"</span>",1));
                }
                updateLog(Html.fromHtml("<br>",1));
            }
        }
        return true;
    }
}
