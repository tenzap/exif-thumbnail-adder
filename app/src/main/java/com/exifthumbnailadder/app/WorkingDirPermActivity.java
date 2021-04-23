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

import androidx.annotation.Nullable;
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
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class WorkingDirPermActivity extends AppCompatActivity {

    private static final int SELECT_ROOT_FOLDER = 1;
    private static final int CREATE_DOCUMENT = 2;
    private static final int OPEN_DOCUMENT_TREE = 3;

    SharedPreferences  prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working_dir_perm);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_ROOT_FOLDER:
                    Uri treeRootUri = data.getData();

                    // Check if workingdir folder already exists
                    String volumeId = UriUtil.getTVolId(treeRootUri);
                    String workingDir = prefs.getString("working_dir", "ThumbAdder");
                    Uri workingDirUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, volumeId + ":" + workingDir);

                    if (!DocumentFile.fromTreeUri(this, workingDirUri).exists()) {
                        // Continue with creating the folder (it doesn't exist yet)
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
                        intent.putExtra(Intent.EXTRA_TITLE, prefs.getString("working_dir", "ThumbAdder"));

                        DocumentFile file = DocumentFile.fromTreeUri(this, treeRootUri);
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, file.getUri());

                        startActivityForResult(intent, CREATE_DOCUMENT);
                    } else {
                        // Folder exists already, we continue by querying permissions on the tree.
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                        Uri treeRootUri2 = DocumentsContract.buildTreeDocumentUri(treeRootUri.getAuthority(), volumeId+":"+ workingDir);
                        Uri workingDirUri2 = DocumentsContract.buildDocumentUriUsingTree(treeRootUri2, volumeId + ":" + workingDir);

                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, workingDirUri2);
                        startActivityForResult(intent, OPEN_DOCUMENT_TREE);
                    }
                    break;
                case CREATE_DOCUMENT:
                    Uri dirUri = data.getData();

                    // Prepare the wall to get permanent permissions on the folder
                    // we just created
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dirUri);
                    startActivityForResult(intent, OPEN_DOCUMENT_TREE);

                    break;
                case OPEN_DOCUMENT_TREE:
                    Uri treeUri = data.getData();

                    // Store permissions
                    grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    // Call again "checkWorkingDirPermissions"
                    Button b = findViewById(R.id.button_checkPermissions);
                    checkWorkingDirPermissions(b);

                    break;
                default:
                    break;
            }
        }
    }

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
                Log.e("My Log", "No permission for: " + tString);
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
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Starting from android 11, we cannot select the root folder anymore.
                // So we do it differently

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
                    startActivityForResult(intent, OPEN_DOCUMENT_TREE);
                } else {
                    // Create the WorkingDirectory dir at the root of the volume
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
                    intent.putExtra(Intent.EXTRA_TITLE, workingDir);

                    Uri treeRootUri = DocumentsContract.buildTreeDocumentUri(uri.getAuthority(), volumeId+":");
                    Uri treeRootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeRootUri, volumeId+":");
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeRootDocUri);

                    startActivityForResult(intent, CREATE_DOCUMENT);
                }
                /*
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                startActivityForResult(intent, SELECT_ROOT_FOLDER);
            }

                 */
            return;
        }

        Toast t = Toast.makeText(this, getString(R.string.working_dir_perm_permissions_set_retry), Toast.LENGTH_LONG);
        t.show();
        finish();
    }

    public static boolean isWorkingDirPermOk(Context ctx) {
        List<UriPermission> persUriPermList = ctx.getContentResolver().getPersistedUriPermissions();
        Uri uri = WorkingDirPermActivity
                .workingDirPermMissing(PreferenceManager.getDefaultSharedPreferences(ctx), persUriPermList, ctx);
        if (uri == null) {
            return true;
        } else {
            Intent intent = new Intent(ctx, WorkingDirPermActivity.class);
            ctx.startActivity(intent);
            return false;
        }
    }
}