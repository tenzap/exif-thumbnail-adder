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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.List;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class WorkingDirPermActivity extends AppCompatActivity {

    SharedPreferences  prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working_dir_perm);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onDestroy() {
        synchronized (PermissionManager.sync) {
            PermissionManager.sync.notify();
        }
        super.onDestroy();
    }

    // Replacing startActivityForResult
    // https://stackoverflow.com/a/62615065/15401262
    ActivityResultLauncher<Intent> mLauncherCreateDocument = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri dirUri = result.getData().getData();

                        // Prepare the call to get permanent permissions on the folder
                        // we just created
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dirUri);
                        mLauncherOpenDocumentTree.launch(intent);
                    }
                }
            });

    ActivityResultLauncher<Intent> mLauncherOpenDocumentTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri treeUri = result.getData().getData();

                        // Store permissions
                        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        // Call again "checkWorkingDirPermissions"
                        Button b = findViewById(R.id.button_checkPermissions);
                        checkWorkingDirPermissions(b);
                    }
                }
            });

    public static Uri workingDirPermMissing(SharedPreferences prefs, List<UriPermission> persUriPermList, Context con) {

        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
        Uri[] treeUris = inputDirs.toUriArray();

        // Iterate on folders containing source images
        for (int j = 0; j < treeUris.length; j++) {
            // CHECK PERMISSION... If we don't have permission, continue to next volumeDir

            String volumeId = UriUtil.getTVolId(treeUris[j]);
            String workingDir = prefs.getString("working_dir", "ThumbAdder");

            Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(treeUris[j].getAuthority(), volumeId+":"+workingDir);
            Uri workingDirUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, volumeId+":"+workingDir);

            boolean perm_ok = false;
            String tString = workingDirUri.toString();
            for (UriPermission perm : persUriPermList) {
                if (tString.startsWith(perm.getUri().toString())) {
                    if (perm.isReadPermission() && perm.isWritePermission()) {
                        if (DocumentFile.fromTreeUri(con, workingDirUri).exists()) {
                            perm_ok = true;
                            break;
                        }
                    }
                }
            }

            if (!perm_ok) {
                if (enableLog) Log.e(TAG, "No permission for: " + tString);
                treeRootUri = DocumentsContract.buildTreeDocumentUri(treeUris[j].getAuthority(), volumeId + ":");
                DocumentFile file = DocumentFile.fromTreeUri(con, treeRootUri);

                // return the URI of the parent
                return file.getUri();
            }
        }
        return null;
    }

    public void checkWorkingDirPermissions(View view) {

        List<UriPermission> persUriPermList = this.getContentResolver().getPersistedUriPermissions();
        Uri uri = workingDirPermMissing(prefs, persUriPermList, this);
        if (uri != null) {
                String volumeId = UriUtil.getTVolId(uri);
                String workingDir = prefs.getString("working_dir", "ThumbAdder");

                Uri treeWDUri = DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), volumeId+":"+workingDir);
                Uri wDUri = DocumentsContract.buildDocumentUriUsingTree(treeWDUri, volumeId + ":" + workingDir);

                // Use "File" class to check if workingDir exists since it is not possible to
                // check with 'DocumentFile.fromTreeUri(this, wDUri).exists()'
                // Because permission to access root is denied starting from android 11
                // Probably requires READ_EXTERNAL_STORAGE
                File workingDirFile = new File(FileUtil.getFullDocIdPathFromTreeUri(wDUri, this));

                if (workingDirFile.exists()) {
                    // Open document tree on WorkingDirUri to get the permission
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, wDUri);
                    mLauncherOpenDocumentTree.launch(intent);
                } else {
                    // Create the WorkingDirectory dir at the root of the volume
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
                    intent.putExtra(Intent.EXTRA_TITLE, workingDir);

                    Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), volumeId+":");
                    Uri treeRootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, volumeId+":");
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeRootDocUri);
                    mLauncherCreateDocument.launch(intent);
                }

            return;
        }

        finish();
    }

    public static boolean isWorkingDirPermOk(Context ctx) {
        List<UriPermission> persUriPermList = ctx.getContentResolver().getPersistedUriPermissions();
        Uri uri = WorkingDirPermActivity.workingDirPermMissing(
                PreferenceManager.getDefaultSharedPreferences(ctx),
                persUriPermList,
                ctx);
        if (uri == null) {
            return true;
        } else {
            // Launch activity that will help getting the permission
            synchronized (PermissionManager.sync) {
                Intent intent = new Intent(ctx, WorkingDirPermActivity.class);
                ctx.startActivity(intent);
                try {
                    PermissionManager.sync.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Check again that it is ok now
            persUriPermList = ctx.getContentResolver().getPersistedUriPermissions();
            uri = WorkingDirPermActivity.workingDirPermMissing(
                    PreferenceManager.getDefaultSharedPreferences(ctx),
                    persUriPermList,
                    ctx);
            if (uri == null) {
                return true;
            } else {
                return false;
            }
        }
    }
}
