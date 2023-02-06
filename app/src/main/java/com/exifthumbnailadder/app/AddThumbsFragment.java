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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
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
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FilenameFilter;

public class AddThumbsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
    NestedScrollView scrollview = null;
    private boolean stopProcessing = false;
    private boolean isProcessing = false;

    Intent ETAServiceIntent;
    BroadcastReceiver receiver;
    private LocalBroadcastManager broadcaster;

    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        ETAServiceIntent = new Intent(getContext(), AddThumbsService.class);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_FINISHED":
                    case "com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_STOPPED_BY_USER":
                        setIsProcessFalse(null);
                        break;
                    default:
                        break;
                }
            }
        };
        broadcaster = LocalBroadcastManager.getInstance(getContext());
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
        filter.addAction("com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_FINISHED");
        filter.addAction("com.exifthumbnailadder.app.ADD_THUMBS_SERVICE_RESULT_STOPPED_BY_USER");
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
                displayStopButton();
                addThumbsUsingTreeUris(view);
            }
        });
        view.findViewById(R.id.button_stopProcess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().stopService(ETAServiceIntent);
            }
        });

        textViewLog = (TextView)view.findViewById(R.id.textview_log);
        textViewDirList = (TextView)view.findViewById(R.id.textview_dir_list);
        scrollview = ((NestedScrollView)  view.findViewById(R.id.scrollview));
        AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);

        LinearLayout ll = (LinearLayout)view.findViewById(R.id.block_allFilesAccess);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use of "All Files Access Permissions" may result in rejection from the google play store
            // We use it only to be able to update the attributes of the files (ie timestamps)
            if (PermissionManager.hasAllFilesAccessPermission())
                ll.setVisibility(View.GONE);
            else
                ll.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.GONE);
        }

        // Create the observer which updates the UI.
        final Observer<SpannableStringBuilder> ETAObserver = new Observer<SpannableStringBuilder>() {
            @Override
            public void onChanged(@Nullable final SpannableStringBuilder spannableLog) {
                // Update the UI, in this case, a TextView.
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            textViewLog.setText(spannableLog);
                        } catch (IndexOutOfBoundsException e) {
                            // TODO: Dirty hack to silent this android exception happening at times during tests
                            if (e.getMessage().equals("setSpan (0 ... -1) has end before start")) {
                                e.printStackTrace();
                            } else {
                                throw e;
                            }
                        }
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
        };
        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        AddThumbsLogLiveData.get().observe(getViewLifecycleOwner(), ETAObserver);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("srcUris")) {
            AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ServiceUtil.isServiceRunning(getContext(), AddThumbsService.class)) {
            displayStopButton();
        } else {
            if (isProcessing)
                displayStopButton();
            else
                displayStartButton();
        }

        getActivity().setTitle(R.string.action_add_thumbs);
        textViewLog.setText(AddThumbsLogLiveData.get().getValue());
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

    public void addThumbsUsingTreeUris(View view) {
        isProcessing = true;
        stopProcessing = false;

        // Set IdlingResource for WorkingDirPermActivity to false so that
        // the tests wait until the Activity is displayed.
        MainActivity.setWorkingDirPermIdlingResourceState(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                AddThumbsLogLiveData.get().clear();
                AddThumbsLogLiveData.get().appendLog(getString(R.string.frag1_log_starting));

                PermissionManager pm = new PermissionManager(getParentFragment(),
                        requestPermissionLauncher);

                if(pm.checkPermissions()) {
                    // Launch service
                    getContext().startForegroundService(ETAServiceIntent);
                } else {
                    setIsProcessFalse(view);
                    stopProcessing = false;
                }
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

    public void setIsProcessFalse(View view) {
        isProcessing = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayStartButton();
                sendFinished();
            }
        });
    }

    public ActivityResultLauncher<String> getRequestPermissionLauncher() {
        return requestPermissionLauncher;
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    PermissionManager.isPermissionGranted = true;
                } else {
                    PermissionManager.isPermissionGranted = false;
                }
                // In case the permission is auto-granted without dialog,
                // we have to set idling resource state to 'idle'
                if (!MainActivity.getIdlingResource().isIdleNow()) {
                    if (PermissionManager.logIdlingResourceChanges)
                        Log.d("ETA", "setIdlingResourceState: true (now auto-grant is done)");
                    MainActivity.setIdlingResourceState(true);
                }
                synchronized(PermissionManager.sync) {
                    PermissionManager.sync.notify();
                }
            });

    private void displayStartButton() {
        Button start = (Button) getView().findViewById(R.id.button_addThumbs);
        Button stop = (Button) getView().findViewById(R.id.button_stopProcess);
        start.setVisibility(Button.VISIBLE);
        stop.setVisibility(Button.GONE);
        setBottomBarMenuItemsEnabled(true);
    }

    private void displayStopButton() {
        Button start = (Button) getView().findViewById(R.id.button_addThumbs);
        Button stop = (Button) getView().findViewById(R.id.button_stopProcess);
        start.setVisibility(Button.GONE);
        stop.setVisibility(Button.VISIBLE);
        setBottomBarMenuItemsEnabled(false);
    }

    private void sendFinished() {
        Intent intent = new Intent("com.exifthumbnailadder.app.ADD_THUMBS_FRAGMENT_FINISHED");
        broadcaster.sendBroadcast(intent);
    }
}
