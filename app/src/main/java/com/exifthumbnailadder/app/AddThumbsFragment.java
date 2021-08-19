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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FilenameFilter;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class AddThumbsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
    public final static SpannableStringBuilder log = new SpannableStringBuilder("");
    NestedScrollView scrollview = null;
    private boolean stopProcessing = false;
    private boolean isProcessing = false;
    private boolean hasWriteExternalStoragePermission = false;
    private boolean continueWithoutWriteExternalStoragePermission = false;

    Intent ETAServiceIntent;
    BroadcastReceiver receiver;

    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        ETAServiceIntent = new Intent(getContext(), ETAService.class);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.SERVICE_RESULT_STRING":
                        String string = intent.getStringExtra("com.exifthumbnailadder.app.SERVICE_MESSAGE");
                        updateUiLog(string);
                        break;
                    case "com.exifthumbnailadder.app.SERVICE_RESULT_SPANNED":
                        Spanned spanned = (Spanned)intent.getCharSequenceExtra("com.exifthumbnailadder.app.SERVICE_MESSAGE");
                        updateUiLog(spanned);
                        break;
                    case "com.exifthumbnailadder.app.SERVICE_RESULT_FINISHED":
                        setIsProcessFalse(null);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_thumbs, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.SERVICE_RESULT_STRING");
        filter.addAction("com.exifthumbnailadder.app.SERVICE_RESULT_SPANNED");
        filter.addAction("com.exifthumbnailadder.app.SERVICE_RESULT_FINISHED");
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        super.onStop();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Context fragmentContext = (MainActivity) view.getContext();

        view.findViewById(R.id.button_addThumbs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button start = (Button) getView().findViewById(R.id.button_addThumbs);
                Button stop = (Button) getView().findViewById(R.id.button_stopProcess);

                start.setVisibility(Button.GONE);
                stop.setVisibility(Button.VISIBLE);

                setBottomBarMenuItemsEnabled(false);
                addThumbsUsingTreeUris(view);
            }
        });
        view.findViewById(R.id.button_stopProcess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProcessing = true;
                getContext().stopService(ETAServiceIntent);
                Button start = (Button) getView().findViewById(R.id.button_addThumbs);
                Button stop = (Button) getView().findViewById(R.id.button_stopProcess);
                start.setVisibility(Button.VISIBLE);
                stop.setVisibility(Button.GONE);
                setBottomBarMenuItemsEnabled(true);
            }
        });

        textViewLog = (TextView)view.findViewById(R.id.textview_log);
        textViewDirList = (TextView)view.findViewById(R.id.textview_dir_list);
        scrollview = ((NestedScrollView)  view.findViewById(R.id.scrollview));
        AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);

        LinearLayout ll = (LinearLayout)view.findViewById(R.id.block_allFilesAccess);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !BuildConfig.FLAVOR.equals("google_play")) {
            // Use of "All Files Access Permissions" may result in rejection from the google play store
            // We use it only to be able to update the attributes of the files (ie timestamps)
            if (MainActivity.haveAllFilesAccessPermission())
                ll.setVisibility(View.GONE);
            else
                ll.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.GONE);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("srcUris")) {
            AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.action_add_thumbs);
        textViewLog.setText(log);
        AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);
        scrollDown();
    }

    private void setBottomBarMenuItemsEnabled(boolean enabled) {
        MenuView.ItemView item1 = getActivity().findViewById(R.id.AddThumbsFragment);
        item1.setEnabled(enabled);
        MenuView.ItemView item2 = getActivity().findViewById(R.id.SyncFragment);
        item2.setEnabled(enabled);
        MenuView.ItemView item3 = getActivity().findViewById(R.id.SettingsFragment);
        item3.setEnabled(enabled);

        // TODO: grey out
        // Some ideas: https://stackoverflow.com/questions/9642990/is-it-possible-to-grey-out-not-just-disable-a-menuitem-in-android
        //item2.getItemData().getIcon().setAlpha(enabled ? 255 : 64);

//        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottomNavigationView);
//        Menu menu = bottomNavigationView.getMenu();
//        MenuItem item = menu.getItem(2);
//        int current = bottomNavigationView.getSelectedItemId();
    }

    private void scrollDown() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
            //if (enableLog) Log.i(TAG, "D: " + dir.toString() + " N: " + name + " " + dirTrailing + " recurseCheckAccept? " + recurseCheckAccept);

            if (dirTrailing.equals(this.acceptedPath) && recurseCheckAccept) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }
    }

    private boolean hasWriteExternalStorage() {
        if (ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            hasWriteExternalStoragePermission = true;
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(R.string.frag1_perm_request_title);
            if (prefs.getBoolean("useSAF", true) &&
                    ( BuildConfig.FLAVOR.equals("google_play") ||
                            (BuildConfig.FLAVOR.equals("standard") && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q))) {
                alertBuilder.setMessage(R.string.frag1_perm_request_message_timestamp);
                alertBuilder.setNegativeButton(R.string.frag1_perm_request_deny, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        continueWithoutWriteExternalStoragePermission = true;
                    }
                });
            } else {
                alertBuilder.setMessage(R.string.frag1_perm_request_message_Files);
            }
            alertBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            });
            alertBuilder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    //do nothing
                }
            });

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                }
            });
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return hasWriteExternalStoragePermission;
    }

    public void addThumbsUsingTreeUris(View view) {
        isProcessing = true;
        stopProcessing = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                log.clear();
                updateUiLog(getString(R.string.frag1_log_starting));

                if (!prefs.getBoolean("useSAF", true) || BuildConfig.FLAVOR.equals("google_play")
                        || BuildConfig.FLAVOR.equals("standard") && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                {
                    updateUiLog(Html.fromHtml(getString(R.string.frag1_check_write_perm), 1));
                    if  (prefs.getBoolean("useSAF", true) && continueWithoutWriteExternalStoragePermission) {
                        updateUiLog(Html.fromHtml("<span style='color:blue'>"+getString(R.string.frag1_continue_without_timestamps)+"</span><br>", 1));
                    } else if (!hasWriteExternalStorage()) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_unsuccessful)+"</span><br>", 1));
                        setIsProcessFalse(view);
                        stopProcessing = false;
                        return;
                    } else {
                        updateUiLog(Html.fromHtml("<span style='color:green'>" + getString(R.string.frag1_log_successful) + "</span><br>", 1));
                    }
                }

                {
                    updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_workingdir_perm), 1));
                    if (!WorkingDirPermActivity.isWorkingDirPermOk(getContext())) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_unsuccessful)+"</span><br>", 1));
                        setIsProcessFalse(view);
                        stopProcessing = false;
                        return;
                    }
                    updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_successful)+"</span><br>", 1));
                }

                getContext().startForegroundService(ETAServiceIntent);
            }
        }).start();
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
                setBottomBarMenuItemsEnabled(true);
            }
        });
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    hasWriteExternalStoragePermission = true;
                } else {
                    hasWriteExternalStoragePermission = false;
                }
            });

}
