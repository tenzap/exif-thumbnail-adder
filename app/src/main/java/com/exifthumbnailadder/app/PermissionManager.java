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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionManager {

    public static final boolean logIdlingResourceChanges = MainApplication.enableLog;

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

        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.frag1_log_checking_perm) + "<br>", 1));

        for (String permission : getRequiredPermissions(prefs)) {
            if (logIdlingResourceChanges)
                Log.d("ETA", "setIdlingResourceState: false (" + permission + ")");
            MainActivity.setIdlingResourceState(false);
            if(!checkPermission(permission))
                hasMissingPermission = true;
        }

        if (!checkWorkingDirPermission())
            hasMissingPermission = true;

        return !hasMissingPermission;
    }

    public boolean checkPermission(String permission) {
        String label, outcome_success, outcome_failure;
        label = "⋅ " + getPermissionLabel(permission) + ":";
        switch (permission) {
            case Manifest.permission.POST_NOTIFICATIONS:
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.allowed) + "</span><br>";
                outcome_failure = "<span style='color:blue'>" + fragment.getString(R.string.denied) + "</span><br>";
                break;
            case Manifest.permission.MANAGE_EXTERNAL_STORAGE:
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.enabled) + "</span><br>";
                outcome_failure = "<span style='color:red'>" + fragment.getString(R.string.denied) + " " + fragment.getString(R.string.can_be_changed_in_settings) + "</span><br>";
                break;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.READ_MEDIA_IMAGES:
            case Manifest.permission.ACCESS_MEDIA_LOCATION:
            default:
                outcome_success = "<span style='color:green'>" + fragment.getString(R.string.allowed) + "</span><br>";
                outcome_failure = "<span style='color:red'>" + fragment.getString(R.string.denied) + "</span><br>";
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
        AddThumbsLogLiveData.get().appendLog(Html.fromHtml(fragment.getString(R.string.frag1_log_checking_workingdir_perm_v2), 1));
        if (WorkingDirPermActivity.isWorkingDirPermOk(fragment.getContext())) {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + fragment.getString(R.string.allowed) + "</span><br>", 1));
            return true;
        } else {
            AddThumbsLogLiveData.get().appendLog(Html.fromHtml("<span style='color:red'>" + fragment.getString(R.string.denied) + "</span><br>", 1));
        }
        return false;
    }

    private boolean requestPermission(String permission) {
        if (PermissionManager.isPermissionGranted(fragment.getContext(), permission)) {
            isPermissionGranted = true;
            // Release the Idling Resource so that the test suite can continue
            if (logIdlingResourceChanges)
                Log.d("ETA", "setIdlingResourceState: true (" + permission + ") - PermissingAlreadyGranted");
            MainActivity.setIdlingResourceState(true);
            return isPermissionGranted;
        }

        if (fragment.shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(fragment.getContext());
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(R.string.frag1_perm_request_title);

            int msg_id, msg_positive_button, msg_neutral_button, msg_negative_button;
            String message = null;

            switch (permission) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    if (prefs.getBoolean("useSAF", true) && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                        message = fragment.getString(R.string.perm_request_message_WRITE_EXTERNAL_STORAGE_for_timestamp, getPermissionLabel(permission));
                    else
                        message = fragment.getString(R.string.perm_request_message_WRITE_EXTERNAL_STORAGE_using_Files_API, getPermissionLabel(permission));
                    break;
                case Manifest.permission.POST_NOTIFICATIONS:
                    message = fragment.getString(R.string.perm_request_message_POST_NOTIFICATIONS, getPermissionLabel(permission));
                    break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                    message = fragment.getString(R.string.perm_request_message_READ_EXTERNAL_STORAGE, getPermissionLabel(permission));
                    break;
                case Manifest.permission.READ_MEDIA_IMAGES:
                    message = fragment.getString(R.string.perm_request_message_READ_EXTERNAL_STORAGE, getPermissionLabel(permission));
                    break;
                case Manifest.permission.ACCESS_MEDIA_LOCATION:
                    message = fragment.getString(R.string.perm_request_message_ACCESS_MEDIA_LOCATION, getPermissionLabel(permission));
                    break;
                default:
                    message = "No label here. Please report a bug.";
                    break;
            }

            msg_positive_button = android.R.string.ok;
            msg_neutral_button = android.R.string.cancel;
            msg_negative_button = R.string.frag1_perm_request_deny;

            alertBuilder.setMessage(message);
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
                        // Window is ready for click in the test suite
                        if (logIdlingResourceChanges)
                            Log.d("ETA", "setIdlingResourceState: true (" + permission + ") - shouldShowRequestPermissionRationale");
                        MainActivity.setIdlingResourceState(true);
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

                // Window is ready for click in the test suite
                // But in some cases, permissions will be automatically granted.
                // For this, we don't set idlingResourceState to true
                // It will be done in the registerForActivityResult
                if (!willPermissionBeAutogranted(permission)) {
                    if (logIdlingResourceChanges)
                        Log.d("ETA", "setIdlingResourceState: true (" + permission + ") - launch permission request dialog");
                    MainActivity.setIdlingResourceState(true);
                } else {
                    if (logIdlingResourceChanges)
                        Log.d("ETA", "setIdlingResourceState: NO CHANGE (" + permission + ") - permission will be auto-granted");
                }
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
                s.add(Manifest.permission.READ_EXTERNAL_STORAGE);
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
        }

        // 3. Permission for notifications
        if (Build.VERSION.SDK_INT >= 33) {
            s.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return s;
    }

    private boolean willPermissionBeAutogranted(String permission) {
        /*
         * These pairs of permissions are auto-granted. If one of the pair
         * is granted, the other one will be granted automatically
         *
         *       ACCESS_MEDIA_LOCATION (API >= 29) & READ_MEDIA_IMAGES (API >= 33)
         *       ACCESS_MEDIA_LOCATION (API >= 29) & READ_EXTERNAL_STORAGE (API <= 32)
         *       ACCESS_MEDIA_LOCATION (API >= 29) & WRITE_EXTERNAL_STORAGE (API <= 29)
         */

        if (Build.VERSION.SDK_INT < 29)
            return false;

        ArrayList<String> a = new ArrayList<>(
                Arrays.asList(
                        Manifest.permission.ACCESS_MEDIA_LOCATION
                ));

        ArrayList<String> b = new ArrayList<>(
                Arrays.asList(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ));

        List<String> grantedPerm = getGrantedPermissions(fragment.getContext().getPackageName());

        if (a.contains(permission)) {
            for (String perm2 : b) {
                if (grantedPerm.contains(perm2))
                    return true;
            }
        }

        if (b.contains(permission)) {
            for (String perm2 : a) {
                if (grantedPerm.contains(perm2))
                    return true;
            }
        }
        return false;
    }

    List<String> getGrantedPermissions(final String appPackage) {
        List<String> granted = new ArrayList<String>();
        try {
            PackageInfo pi = fragment.getContext().getPackageManager().getPackageInfo(appPackage, PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(pi.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return granted;
    }

    public String getPermissionLabel(String permission) {
        try {
            PermissionInfo pinfo = fragment.getContext().getPackageManager().getPermissionInfo(permission, PackageManager.GET_META_DATA);
            String label = pinfo.loadLabel(fragment.getContext().getPackageManager()).toString();
            if (label.equals("android.permission.MANAGE_EXTERNAL_STORAGE"))
                return fragment.getString(R.string.perm_label_MANAGE_EXTERNAL_STORAGE);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            return permission;
        }
    }
}
