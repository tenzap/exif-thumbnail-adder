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

import java.util.ArrayList;

public class PermissionManager {

    Fragment fragment;
    SharedPreferences prefs;

    public static boolean isPermissionGranted = false;

    private boolean continueWithoutWriteExternalStoragePermission = false;

    ActivityResultLauncher<String> requestPermissionLauncher;

    final public static Object sync = new Object();

    PermissionManager(Fragment fragment, ActivityResultLauncher<String> requestPermissionLauncher) {
        this.requestPermissionLauncher = requestPermissionLauncher;
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

    public static boolean isPermissionGranted(Context ctx, String permission) {
        if (permission.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            return hasAllFilesAccessPermission();
        }

        return ContextCompat.checkSelfPermission(
                ctx,
                permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasWriteExternalStorageOrAllFilesAccess(Context ctx) {
        return PermissionManager.isPermissionGranted(ctx, "WRITE_EXTERNAL_STORAGE") || hasAllFilesAccessPermission();
    }

    public boolean checkPermissions() {
        boolean hasMissingPermission = false;

        for (String permission : getRequiredPermissions(prefs)) {
            if(!checkPermission(permission))
                hasMissingPermission = true;
        }

        if (!checkWorkingDirPermission())
            hasMissingPermission = true;

        return !hasMissingPermission;
    }

    private boolean checkPermission(String permission) {
        String label, outcome_success, outcome_failure;
        switch (permission) {
            case Manifest.permission.POST_NOTIFICATIONS:
                label = fragment.getString(R.string.notification_status);
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.enabled) + "</span><br>";
                outcome_failure = "<span style='color:blue'>" + fragment.getString(R.string.disabled) + "</span><br>";
                break;
            case Manifest.permission.MANAGE_EXTERNAL_STORAGE:
                label = "Checking 'All files access' permission";
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.enabled) + "</span><br>";
                outcome_failure = "<span style='color:red'>" + "Disabled. This can be changed in the settings." + "</span><br>";
                break;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.ACCESS_MEDIA_LOCATION:
            default:
                label = fragment.getString(R.string.frag1_check_write_perm);
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.frag1_log_successful) + "</span><br>";
                outcome_failure = "<span style='color:red'>" + fragment.getString(R.string.frag1_log_unsuccessful) + "</span><br>";
                break;
        }

        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(label,1));

        if (requestPermission(permission)) {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml(outcome_success,1));
            return true;
        }

        if (prefs.getBoolean("useSAF", true)) {
            if (Build.VERSION.SDK_INT <= 29) {
                if (continueWithoutWriteExternalStoragePermission) {
                    AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:blue'>" + fragment.getString(R.string.frag1_continue_without_timestamps) + "</span><br>", 1));
                    return true;
                }
            }
        }

        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(outcome_failure, 1));

        // Not having notification permission shouldn't block processing of pictures.
        // So return true
        if (permission.equals(Manifest.permission.POST_NOTIFICATIONS))
            return true;

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

    private boolean requestPermission(String permission) {
        if (PermissionManager.isPermissionGranted(fragment.getContext(), permission)) {
            isPermissionGranted = true;
            return isPermissionGranted;
        }

        if (fragment.shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(fragment.getContext());
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(R.string.frag1_perm_request_title);

            int msg_id, msg_positive_button, msg_neutral_button, msg_negative_button;

            switch (permission) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    if (prefs.getBoolean("useSAF", true) && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                        msg_id = R.string.frag1_perm_request_message_timestamp;
                    else
                        msg_id = R.string.frag1_perm_request_message_Files;
                    break;
                case Manifest.permission.POST_NOTIFICATIONS:
                    msg_id = R.string.notification_permission_message;
                    break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                case Manifest.permission.READ_MEDIA_IMAGES:
                case Manifest.permission.ACCESS_MEDIA_LOCATION:
                default:
                    msg_id = R.string.frag1_perm_request_message_Files;
                    break;
            }

            msg_positive_button = android.R.string.ok;
            msg_neutral_button = android.R.string.cancel;
            msg_negative_button = R.string.frag1_perm_request_deny;

            alertBuilder.setMessage(msg_id);
            alertBuilder.setPositiveButton(msg_positive_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (sync) {
                        requestPermissionLauncher.launch(permission);
                    }
                }
            });
            alertBuilder.setNeutralButton(msg_neutral_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (sync) {
                        sync.notify();
                    }
                }
            });
            alertBuilder.setNegativeButton(msg_negative_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (sync) {
                        if (prefs.getBoolean("useSAF", true) &&
                                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                            continueWithoutWriteExternalStoragePermission = true;
                        }
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
                requestPermissionLauncher.launch(permission);
                try {
                    sync.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return isPermissionGranted;
    }

    public static ArrayList<String> getRequiredPermissions(SharedPreferences prefs) {
        ArrayList<String> s = new ArrayList<>();

        // 1. permissions for access to the files and proper processing

        // API <= 29
        //  - if using SAF: WRITE_EXTERNAL_STORAGE to update the timestamps
        //  - if using Files: WRITE_EXTERNAL_STORAGE to be able to write the files with 'Files'
        if (Build.VERSION.SDK_INT <= 29) {
            s.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // API 30-32
        //  - if using SAF:
        //      none (+ optionally MANAGE_EXTERNAL_STORAGE to write timestamps)
        //  - if using SAF + libexif:
        //      like SAF only + READ_EXTERNAL_STORAGE  (+ optionally MANAGE_EXTERNAL_STORAGE to write timestamps)
        //  - if using MediaStore
        //      probably relevant only for files in MediaStore, ie DCIM & Pictures only.
        //      READ_EXTERNAL_STORAGE + writeRequest as described here:
        //       + writeRequest as described here: https://developer.android.com/training/data-storage/shared/media?hl=fr#manage-groups-files
        //  - if using Files:
        //      MANAGE_EXTERNAL_STORAGE/All files access
        //
        if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT <= 32) {
            if (prefs.getBoolean("useSAF", true)) {
                if (prefs.getString("exif_library", "").equals("exiflib_libexif")) {
                    s.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            } else {
                s.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            }
        }

        // API 33:
        //  = Like API 30-32 except we use READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= 33) {
            if (prefs.getBoolean("useSAF", true)) {
                if (prefs.getString("exif_library", "").equals("exiflib_libexif")) {
                    s.add(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                s.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            }
        }

        // 2. Permission to access the Exif tags related to GPS location
        if (Build.VERSION.SDK_INT >= 29) {
            s.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
            // TODO: Make use of setRequireOriginal when using the MediaStore
            // https://developer.android.com/reference/android/provider/MediaStore#setRequireOriginal(android.net.Uri)
        }

        // 3. Permission for notifications
        if (Build.VERSION.SDK_INT >= 33) {
            s.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return s;
    }
}
