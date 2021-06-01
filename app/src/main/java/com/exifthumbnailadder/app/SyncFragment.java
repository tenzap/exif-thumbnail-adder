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
import androidx.appcompat.view.menu.MenuView;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.TreeSet;

public class SyncFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
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
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        view.findViewById(R.id.sync_button_list_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                setBottomBarMenuItemsEnabled(false);
                doSync(true);
            }
        });

        view.findViewById(R.id.sync_button_del_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                setBottomBarMenuItemsEnabled(false);
                doSync(false);
            }
        });

        view.findViewById(R.id.sync_button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProcessing = true;
                setBottomBarMenuItemsEnabled(true);
                displayStartButton();
            }
        });

        textViewLog = (TextView)view.findViewById(R.id.sync_textview_log);
        textViewDirList = (TextView)view.findViewById(R.id.sync_textview_dir_list);
        scrollview = ((ScrollView)view.findViewById(R.id.sync_scrollview));
        AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("srcUris")) {
            AddThumbsFragment.updateTextViewDirList(getContext(), textViewDirList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.action_sync);
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

    public void doSync(boolean dryRun) {
        isProcessing = true;
        stopProcessing = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                log.clear();
                updateUiLog(getString(R.string.frag1_log_starting));

                {
                    updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_workingdir_perm), 1));
                    if (!WorkingDirPermActivity.isWorkingDirPermOk(getContext())) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_ko)+"</span><br>", 1));
                        setIsProcessFalse();
                        stopProcessing = false;
                        return;
                    }
                    updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_ok)+"</span><br>", 1));
                }

                InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
                Object[] srcDirs;
                if (prefs.getBoolean("useSAF", true)) {
                    srcDirs = inputDirs.toUriArray(); // Uri[]
                } else {
                    srcDirs = inputDirs.toFileArray(getContext()); // File[]
                }

                // Iterate on folders containing source images
                for (int j = 0; j < srcDirs.length; j++) {
                    ETASrcDir etaSrcDir = null;
                    if (srcDirs[j] instanceof Uri) {
                        etaSrcDir = new ETASrcDirUri(getContext(), (Uri)srcDirs[j]);
                    } else if (srcDirs[j] instanceof File) {
                        etaSrcDir = new ETASrcDirFile(getContext(), (File)srcDirs[j]);
                    }
                    if (etaSrcDir == null) throw new UnsupportedOperationException();

                    updateUiLog(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, etaSrcDir.getFSPath()) + "</b></u><br>",1));

                    // Check permission in case we use SAF...
                    // If we don't have permission, continue to next srcDir
                    updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_perm), 1));
                    if (! etaSrcDir.isPermOk()) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_not_granted)+"</span><br>", 1));
                        continue;
                    }
                    updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_ok)+"</span><br>", 1));


                    ETADoc etaDocSrc = null;
                    if (etaSrcDir instanceof ETASrcDirUri) {
                        DocumentFile baseDf = DocumentFile.fromTreeUri(getContext(), (Uri)srcDirs[j]);
                        etaDocSrc = new ETADocDf(baseDf, getContext(), (ETASrcDirUri)etaSrcDir, false);
                    } else if (etaSrcDir instanceof ETASrcDirFile) {
                        etaDocSrc = new ETADocFile((File)srcDirs[j], getContext(), (ETASrcDirFile)etaSrcDir, true);
                    }
                    if (etaDocSrc == null) throw new UnsupportedOperationException();

                    // Process backupUri
                    ETASrcDir etaSrcDirBackup = null;
                    if (etaDocSrc instanceof ETADocDf) {
                        etaSrcDirBackup = new ETASrcDirUri(
                                getContext(),
                                etaDocSrc.getBackupUri());
                    } else if (etaDocSrc instanceof ETADocFile) {
                        etaSrcDirBackup = new ETASrcDirFile(
                                getContext(),
                                etaDocSrc.getBackupPath().toFile());
                    }
                    doSyncForUri(etaSrcDirBackup, etaDocSrc, dryRun);

                    // Process outputUri
                    if (!PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("writeThumbnailedToOriginalFolder", false)) {
                        updateUiLog(Html.fromHtml("<br>",1));
                        ETASrcDir etaSrcDirDest = null;
                        if (etaDocSrc instanceof ETADocDf) {
                            etaSrcDirDest = new ETASrcDirUri(
                                    getContext(),
                                    etaDocSrc.getDestUri());
                        } else if (etaDocSrc instanceof ETADocFile) {
                            etaSrcDirDest = new ETASrcDirFile(
                                    getContext(),
                                    etaDocSrc.getDestPath().toFile());
                        }
                        doSyncForUri(etaSrcDirDest, etaDocSrc, dryRun);
                    }
                }

                updateUiLog(getString(R.string.frag1_log_finished));

                setIsProcessFalse();
            }
        }).start();
    }

    private void doSyncForUri(ETASrcDir workingDirDocs, ETADoc srcDirEtaDoc, boolean dryRun ) {

        TreeSet<Object> docsInWorkingDir = (TreeSet<Object>)workingDirDocs.getDocsSet();

        updateUiLog(getString(R.string.sync_log_checking, workingDirDocs.getFSPathWithoutRoot()));
        updateUiLog("\n");
        updateUiLog(Html.fromHtml("<u>" + getString(R.string.sync_log_files_to_remove) + "</u><br>",1));

        for (Object _doc : docsInWorkingDir) {

            // Convert (Object)_doc to (Uri)doc or (File)doc
            ETADoc doc = null;
            if (workingDirDocs.getDocsRoot() instanceof Uri) {
                doc = new ETADocDf((DocumentFile) _doc, getContext(), (ETASrcDirUri) workingDirDocs, false);
            } else if (workingDirDocs.getDocsRoot() instanceof File) {
                doc = new ETADocFile((File) _doc, getContext(), (ETASrcDirFile)workingDirDocs, true);
            }
            if (doc == null) throw new UnsupportedOperationException();

            if (stopProcessing) {
                setIsProcessFalse();
                stopProcessing = false;
                updateUiLog(Html.fromHtml("<br><br>"+getString(R.string.frag1_log_stopped_by_user),1));
                return;
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
                    srcFileExists = DocumentFile.fromTreeUri(getContext(), srcUri).exists();
                } else if (workingDirDocs.getDocsRoot() instanceof File) {
                    File srcFile = doc.getSrcPath(srcDirEtaDoc.getFullFSPath()).toFile();
                    srcFileExists = srcFile.exists();
                }
            } catch (Exception e) {
                updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.toString()) + "</span><br>", 1));
                e.printStackTrace();
                continue;
            }

            // Delete file if it doesn't exist in source directory
            if (!srcFileExists) {
                updateUiLog("⋅ " + doc.getDPath() + "... ");
                if (!dryRun) {
                    boolean deleted = doc.delete();
                    if (deleted)
                        updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_done)+"</span>",1));
                    else
                        updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.sync_log_failure_to_delete_file)+"</span>",1));
                }
                updateUiLog(Html.fromHtml("<br>",1));
            }
        }
    }

    public void updateUiLog(String text) {
        if (MainApplication.enableLog) Log.i(MainApplication.TAG, text);
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
        if (MainApplication.enableLog) Log.i(MainApplication.TAG, text.toString());
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

    public void setIsProcessFalse() {
        isProcessing = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayStartButton();
                setBottomBarMenuItemsEnabled(true);
            }
        });
    }

    public void displayStopButton() {
        Button list = (Button) getView().findViewById(R.id.sync_button_list_files);
        Button start = (Button) getView().findViewById(R.id.sync_button_del_files);
        Button stop = (Button)getView().findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.GONE);
        start.setVisibility(Button.GONE);
        stop.setVisibility(Button.VISIBLE);
    }

    public void displayStartButton() {
        Button list = (Button) getView().findViewById(R.id.sync_button_list_files);
        Button start = (Button) getView().findViewById(R.id.sync_button_del_files);
        Button stop = (Button) getView().findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.VISIBLE);
        start.setVisibility(Button.VISIBLE);
        stop.setVisibility(Button.GONE);
    }

}
