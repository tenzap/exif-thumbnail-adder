/*
 * Copyright (C) 2023 Fab Stz <fabstz-it@yahoo.fr>
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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.Html;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class PermissionManager {

    Fragment fragment;
    SharedPreferences prefs;

    public static boolean hasWriteExternalStoragePermission = false;
    public static boolean hasPostNotificationPermission = false;

    private boolean continueWithoutWriteExternalStoragePermission = false;

    ActivityResultLauncher<String> requestPermissionLauncher;
    ActivityResultLauncher<String> requestPostNotificationPermissionLauncher;

    final public static Object sync = new Object();

    PermissionManager(Fragment fragment, ActivityResultLauncher<String> requestPermissionLauncher,
                      ActivityResultLauncher<String> requestPostNotificationPermissionLauncher) {
        this.requestPermissionLauncher = requestPermissionLauncher;
        this.requestPostNotificationPermissionLauncher = requestPostNotificationPermissionLauncher;
        this.fragment = fragment;
        prefs = PreferenceManager.getDefaultSharedPreferences(fragment.getContext());
    }

    public static boolean manifestHasMANAGE_EXTERNAL_STORAGE(Context ctx) {
        try {
            String[] requestedPermissions = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            for (String requestedPermission : requestedPermissions) {
                if (requestedPermission.equals("android.permission.MANAGE_EXTERNAL_STORAGE"))
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @TargetApi(30)
    public static boolean hasAllFilesAccessPermission() {
        return Environment.isExternalStorageManager();
    }

    public static boolean hasWriteExternalStorage(Context ctx) {
        // WRITE_EXTERNAL_STORAGE is available only for API <= 29
        if (Build.VERSION.SDK_INT <= 29) {
            return ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    public static boolean hasWriteExternalStorageOrAllFilesAccess(Context ctx) {
        return PermissionManager.hasWriteExternalStorage(ctx) || hasAllFilesAccessPermission();
    }

    public static boolean hasReadExternalStorage(Context ctx) {
        // READ_EXTERNAL_STORAGE is available only for API <= 32
        if (Build.VERSION.SDK_INT <= 32) {
            return ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    public static boolean hasPostNotifications(Context ctx) {
        return ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public boolean checkPermissions() {
        boolean writeExternalStorageSufficient = checkWriteExternalStoragePermission();
        boolean workingDirSufficient = checkWorkingDirPermission();
        boolean notificationSufficient = checkNotificationPermission();

        if (writeExternalStorageSufficient &&
                workingDirSufficient &&
                notificationSufficient)
            return true;
        else
            return false;
    }

    private boolean checkWriteExternalStoragePermission() {
        if (prefs.getBoolean("useSAF", true)) {
            // Request WRITE_EXTERNAL_STORAGE because this is the only way to
            // update the timestamps.
            if (Build.VERSION.SDK_INT <= 29) {
                AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.frag1_check_write_perm), 1));
                if (requestWriteExternalStorage()) {
                    AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + fragment.getString(R.string.frag1_log_successful) + "</span><br>", 1));
                    return true;
                } else {
                    if (continueWithoutWriteExternalStoragePermission) {
                        AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:blue'>" + fragment.getString(R.string.frag1_continue_without_timestamps) + "</span><br>", 1));
                        return true;
                    } else {
                        AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:red'>" + fragment.getString(R.string.frag1_log_unsuccessful) + "</span><br>", 1));
                    }
                }
            } else {
                // For API >= 30, WRITE_EXTERNAL_STORAGE is not available anymore
                // So this is managed by MANAGE_EXTERNAL_STORAGE
                // Return true here so as not to block further processing
                return true;
            }
        } else {
            // Using "Files"
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.frag1_check_write_perm), 1));
            if (!requestWriteExternalStorage()) {
                AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + fragment.getString(R.string.frag1_log_successful) + "</span><br>", 1));
                return true;
            } else {
                AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:red'>" + fragment.getString(R.string.frag1_log_unsuccessful) + "</span><br>", 1));
            }
        }
        return false;
    }

    private boolean checkWorkingDirPermission() {
        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.frag1_log_checking_workingdir_perm), 1));
        if (WorkingDirPermActivity.isWorkingDirPermOk(fragment.getContext())) {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + fragment.getString(R.string.frag1_log_successful) + "</span><br>", 1));
            return true;
        } else {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:red'>" + fragment.getString(R.string.frag1_log_unsuccessful) + "</span><br>", 1));
        }
        return false;
    }

    private boolean checkNotificationPermission() {
        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.notification_status), 1));
        if (requestNotificationPermission()) {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + fragment.getString(R.string.enabled) + "</span><br>", 1));
        } else {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:blue'>" + fragment.getString(R.string.disabled) + "</span><br>", 1));
        }
        // Not having notification permission doesn't block processing of pictures.
        // Always return true
        return true;
    }

    private boolean requestNotificationPermission() {
        if (PermissionManager.hasPostNotifications(fragment.getContext())) {
            hasPostNotificationPermission = true;
            return true;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(fragment.getContext());
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle(R.string.notification_permission_title);
                alertBuilder.setMessage(R.string.notification_permission_message);
                alertBuilder.setNegativeButton(R.string.frag1_perm_request_deny, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (sync) {
                            sync.notify();
                        }
                    }
                });
                alertBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (sync) {
                            requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    }
                });
                alertBuilder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (sync) {
                            sync.notify();
                        }
                    }
                });

                synchronized (sync) {
                    fragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog alert = alertBuilder.create();
                            alert.show();
                        }
                    });
                    // Wait until user answered
                    try {
                        sync.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                synchronized (sync) {
                    requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    try {
                        sync.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        return hasPostNotificationPermission;
    }

    private boolean requestWriteExternalStorage() {
        if (PermissionManager.hasWriteExternalStorage(fragment.getContext())) {
            hasWriteExternalStoragePermission = true;
            return hasWriteExternalStoragePermission;
        }

        if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(fragment.getContext());
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(R.string.frag1_perm_request_title);
            if (prefs.getBoolean("useSAF", true) &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                alertBuilder.setMessage(R.string.frag1_perm_request_message_timestamp);
                alertBuilder.setNegativeButton(R.string.frag1_perm_request_deny, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (sync) {
                            continueWithoutWriteExternalStoragePermission = true;
                            sync.notify();
                        }
                    }
                });
            } else {
                alertBuilder.setMessage(R.string.frag1_perm_request_message_Files);
            }
            alertBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (sync) {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            });
            alertBuilder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (sync) {
                        sync.notify();
                    }
                }
            });

            synchronized (sync) {
                fragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alert = alertBuilder.create();
                        alert.show();
                    }
                });
                // Wait until user answered
                try {
                    sync.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            synchronized (sync) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                try {
                    sync.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return hasWriteExternalStoragePermission;
    }
}
