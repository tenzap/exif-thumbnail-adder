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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class SyncFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
    public final static SpannableStringBuilder log = new SpannableStringBuilder("");
    ScrollView scrollview = null;
    private boolean stopProcessing = false;
    private boolean isProcessing = false;

    Intent syncServiceIntent;
    BroadcastReceiver receiver;
    private LocalBroadcastManager broadcaster;

    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED":
                    case "com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_STOPPED_BY_USER":
                        setIsProcessFalse();
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
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_FINISHED");
        filter.addAction("com.exifthumbnailadder.app.SYNC_SERVICE_RESULT_STOPPED_BY_USER");
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

        view.findViewById(R.id.sync_button_list_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                doSync(true);
            }
        });

        view.findViewById(R.id.sync_button_del_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                doSync(false);
            }
        });

        view.findViewById(R.id.sync_button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().stopService(syncServiceIntent);
            }
        });

        textViewLog = (TextView) view.findViewById(R.id.sync_textview_log);
        textViewDirList = (TextView) view.findViewById(R.id.sync_textview_dir_list);
        scrollview = ((ScrollView) view.findViewById(R.id.sync_scrollview));
        AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);

        // Create the observer which updates the UI.
        final Observer<SpannableStringBuilder> ETAObserver = new Observer<SpannableStringBuilder>() {
            @Override
            public void onChanged(@Nullable final SpannableStringBuilder spannableLog) {
                // Update the UI, in this case, a TextView.
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String textViewLogStringBeforeUpdate = null;
                        if (BuildConfig.IS_TESTING.get()) {
                            // For Debug purposes: to try to understand this rare exception
                            // java.lang.IndexOutOfBoundsException: setSpan (0 ... -1) has end before start
                            // https://issuetracker.google.com/issues/272576535
                            textViewLogStringBeforeUpdate = textViewLog.getText().toString();
                        }
                        try {
                            textViewLog.setText(spannableLog);
                        } catch (IndexOutOfBoundsException e) {
                            String msg = e.getMessage();
                            if (msg != null && msg.startsWith("setSpan")) {
                                e.printStackTrace();
                                Log.e("ETATest", "Crash with message: " + msg);
                                Log.v("ETATest", "textViewLog Before: " + textViewLogStringBeforeUpdate);
                                Log.v("ETATest", "spannableLog: " + spannableLog);
                                Log.v("ETATest", "textViewLog After: " + textViewLog.getText());
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
        SyncLogLiveData.get().observe(getViewLifecycleOwner(), ETAObserver);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("srcUris")) {
            AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ServiceUtil.isServiceRunning(getContext(), SyncService.class)) {
            displayStopButton();
        } else {
            if (isProcessing)
                displayStopButton();
            else
                displayStartButton();
        }

        getActivity().setTitle(R.string.action_sync);
        textViewLog.setText(SyncLogLiveData.get().getValue());
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

    public void doSync(boolean dryRun) {
        isProcessing = true;
        stopProcessing = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                SyncLogLiveData.get().clear();
                SyncLogLiveData.get().appendLog(getString(R.string.frag1_log_starting));

                SyncLogLiveData.get().appendLog(Html.fromHtml(getString(R.string.frag1_log_checking_perm) + "<br>", 1));

                {
                    SyncLogLiveData.get().appendLog(Html.fromHtml(getString(R.string.frag1_log_checking_workingdir_perm_v2), 1));
                    if (!WorkingDirPermActivity.isWorkingDirPermOk(getContext())) {
                        SyncLogLiveData.get().appendLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.denied) + "</span><br>", 1));
                        setIsProcessFalse();
                        stopProcessing = false;
                        return;
                    }
                    SyncLogLiveData.get().appendLog(Html.fromHtml("<span style='color:green'>" + getString(R.string.allowed) + "</span><br>", 1));
                }

                syncServiceIntent = new Intent(getContext(), SyncService.class);
                syncServiceIntent.putExtra("dryRun", dryRun);
                getContext().startForegroundService(syncServiceIntent);
            }
        }).start();
    }

    public void setIsProcessFalse() {
        isProcessing = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayStartButton();
                sendFinished();
            }
        });
    }

    public void displayStopButton() {
        Button list = (Button) getView().findViewById(R.id.sync_button_list_files);
        Button start = (Button) getView().findViewById(R.id.sync_button_del_files);
        Button stop = (Button) getView().findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.GONE);
        start.setVisibility(Button.GONE);
        stop.setVisibility(Button.VISIBLE);
        setBottomBarMenuItemsEnabled(false);
    }

    public void displayStartButton() {
        Button list = (Button) getView().findViewById(R.id.sync_button_list_files);
        Button start = (Button) getView().findViewById(R.id.sync_button_del_files);
        Button stop = (Button) getView().findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.VISIBLE);
        start.setVisibility(Button.VISIBLE);
        stop.setVisibility(Button.GONE);
        setBottomBarMenuItemsEnabled(true);
    }

    private void sendFinished() {
        Intent intent = new Intent("com.exifthumbnailadder.app.SYNC_FRAGMENT_FINISHED");
        broadcaster.sendBroadcast(intent);
    }
}
