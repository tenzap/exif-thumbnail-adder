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

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.TreeSet;

public class SyncActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs = null;
    TextView textViewLog, textViewDirList;
    Handler mHandler;
    public final static SpannableStringBuilder log = new SpannableStringBuilder("");
    ScrollView scrollview = null;
    private boolean stopProcessing = false;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.sync_button_list_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                doSync(true);
            }
        });

        findViewById(R.id.sync_button_del_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStopButton();
                doSync(false);
            }
        });

        findViewById(R.id.sync_button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopProcessing = true;
                displayStartButton();
            }
        });

        textViewLog = (TextView)findViewById(R.id.sync_textview_log);
        textViewDirList = (TextView)findViewById(R.id.sync_textview_dir_list);
        scrollview = ((ScrollView)findViewById(R.id.sync_scrollview));
        FirstFragment.updateTextViewDirList(this, textViewDirList);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("srcUris")) {
            FirstFragment.updateTextViewDirList(this, textViewDirList);
        }
    }

    public void doSync(boolean dryRun) {
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
                    if (!WorkingDirPermActivity.isWorkingDirPermOk(getApplicationContext())) {
                        updateUiLog(Html.fromHtml("<span style='color:red'>"+getString(R.string.frag1_log_ko)+"</span><br>", 1));
                        setIsProcessFalse();
                        stopProcessing = false;
                        return;
                    }
                    updateUiLog(Html.fromHtml("<span style='color:green'>"+getString(R.string.frag1_log_ok)+"</span><br>", 1));
                }

                String secVolName = FirstFragment.getSecVolumeName(getApplicationContext(), true);
                String secVolDirName = prefs.getString("excluded_sec_vol_prefix", getString(R.string.pref_excludedSecVolPrefix_defaultValue))+secVolName;

                InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
                Uri[] treeUris = inputDirs.toUriArray();

                List<UriPermission> persUriPermList = getContentResolver().getPersistedUriPermissions();

                // Iterate on folders containing source images
                for (int j = 0; j < treeUris.length; j++) {
                    updateUiLog(Html.fromHtml("<br><u><b>"+getString(R.string.frag1_log_processing_dir, FileUtil.getFullPathFromTreeUri(treeUris[j], getApplicationContext())) + "</b></u><br>",1));
                    {
                        // Check permission... If we don't have permission, continue to next volumeDir
                        updateUiLog(Html.fromHtml(getString(R.string.frag1_log_checking_perm), 1));
                        boolean perm_ok = false;
                        String tString = treeUris[j].toString();
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

                    String mainDir = UriUtil.getDD1(treeUris[j]);
                    String subDir = UriUtil.getDDSubParent(treeUris[j]);
                    PathUtil pathUtil = new PathUtil(
                            treeUris[j],
                            mainDir,
                            subDir,
                            UriUtil.getTVolId(treeUris[j]),
                            secVolDirName,
                            prefs);

                    // Process backupUri
                    Uri backupUri = pathUtil.getBackupUri(getApplicationContext(), false);
                    doSyncForUri(backupUri, UriUtil.getTreeId(treeUris[j]), mainDir, secVolDirName, dryRun);

                    // Process outputUri
                    if (!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("writeThumbnailedToOriginalFolder", false)) {
                        Uri outputUri = pathUtil.getDestUri(getApplicationContext());
                        updateUiLog(Html.fromHtml("<br>",1));
                        doSyncForUri(outputUri, UriUtil.getTreeId(treeUris[j]), mainDir, secVolDirName, dryRun);
                    }
                }

                updateUiLog(getString(R.string.frag1_log_finished));

                setIsProcessFalse();
            }
        }).start();
    }

    private void doSyncForUri(Uri backupUri, String srcTreeId,  String mainDir, String excludedDir, boolean dryRun ) {
        ETADocs etaDocs = new ETADocs(this, backupUri, excludedDir);
        TreeSet<DocumentFile> docFilesInBackup = (TreeSet<DocumentFile>)etaDocs.getDocsSet();

        updateUiLog("WorkingDir: ");
        updateUiLog(FileUtil.getFullPathFromTreeUri(backupUri, this));
        updateUiLog("\n");
        updateUiLog(Html.fromHtml("<u>" + getString(R.string.sync_log_files_to_remove) + "</u><br>",1));

        for (DocumentFile doc : docFilesInBackup) {
            if (stopProcessing) {
                setIsProcessFalse();
                stopProcessing = false;
                updateUiLog(Html.fromHtml("<br><br>"+getString(R.string.frag1_log_stopped_by_user),1));
                return;
            }

            Uri srcUri = null;
            try {
                srcUri = PathUtil.getSrcDocumentUriFor(doc.getUri(), mainDir, srcTreeId);
            } catch (Exception e) {
                updateUiLog(Html.fromHtml("<span style='color:red'>" + getString(R.string.frag1_log_skipping_error, e.toString()) + "</span><br>", 1));
                e.printStackTrace();
                continue;
            }

            // Skip some files
            if (doc.isFile()) {
                if (doc.getName().equals(".nomedia")) continue;
                if (!doc.getType().equals("image/jpeg")) continue;
            }
            if (doc.isDirectory()) continue;

            // Delete file if it doesn't exist in source directory
            boolean srcFileExists = DocumentFile.fromTreeUri(this, srcUri).exists();
            if (!srcFileExists) {
                updateUiLog("⋅ " + UriUtil.getDPath(doc.getUri()) + "... ");
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
        runOnUiThread(new Runnable() {
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
        runOnUiThread(new Runnable() {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayStartButton();
            }
        });
    }

    public void displayStopButton() {
        Button list = (Button) findViewById(R.id.sync_button_list_files);
        Button start = (Button) findViewById(R.id.sync_button_del_files);
        Button stop = (Button) findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.GONE);
        start.setVisibility(Button.GONE);
        stop.setVisibility(Button.VISIBLE);
    }

    public void displayStartButton() {
        Button list = (Button) findViewById(R.id.sync_button_list_files);
        Button start = (Button) findViewById(R.id.sync_button_del_files);
        Button stop = (Button) findViewById(R.id.sync_button_stop);
        list.setVisibility(Button.VISIBLE);
        start.setVisibility(Button.VISIBLE);
        stop.setVisibility(Button.GONE);
    }

}
