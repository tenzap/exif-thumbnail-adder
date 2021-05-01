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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.List;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class SettingsActivity extends AppCompatActivity {

    private final int OPEN_DOCUMENT_TREE_RESULT_CODE = 10;
    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        settingsFragment = new SettingsFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, settingsFragment)
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        private boolean doNotUnregisterPreferenceChangedListener = false;

        public void setDoNotUnregisterPreferenceChangedListener(boolean value) {
            doNotUnregisterPreferenceChangedListener = value;
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreferenceCompat allFilesAccess = findPreference("settings_allFilesAccess");
            allFilesAccess.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    requestAllFilesAccessPermission();
                    return true;
                }
            });

            updatePathsSummary();
            setKeepTimeStampOnBackupEnabled();
            setAllFilesAccess();
            setSkipPicsHavingThumbnail();

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            String exif_library = prefs.getString("exif_library", "exiflib_exiv2");
            if (exif_library.equals("exiflib_pixymeta") ||
                    BuildConfig.FLAVOR.equals("pixymeta") ||
                    BuildConfig.BUILD_TYPE.equals("debug")) {
                addPixymetaToLibraryList();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setAllFilesAccess();
        }

        @Override
        public void onPause() {
            if (! doNotUnregisterPreferenceChangedListener) {
                getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            }
            // set value back to true
            doNotUnregisterPreferenceChangedListener = false;
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ( key.equals("srcUris")) {
                updatePathsSummary();
            }
            if (key.equals("writeThumbnailedToOriginalFolder")) {
                setKeepTimeStampOnBackupEnabled();
            }
            if ( key.equals("exif_library")) {
                setSkipPicsHavingThumbnail();
            }
            if ( key.equals("skipPicsHavingThumbnail")) {
                setSkipPicsHavingThumbnail();
            }
        }

        public void setSkipPicsHavingThumbnail() {
            if (getPreferenceManager().getSharedPreferences().getString("exif_library", "exiflib_exiv2").equals("exiflib_android-exif-extended")) {
                if (!getPreferenceManager().getSharedPreferences().getBoolean("skipPicsHavingThumbnail", true)) {
                    SwitchPreferenceCompat skipPicsHavingThumbnailPref = findPreference("skipPicsHavingThumbnail");
                    skipPicsHavingThumbnailPref.setChecked(true);
                    Toast t = Toast.makeText(this.getContext(), getString(R.string.pref_exifLibrary_cannotDisableSkipPicsHavingThumbnail, getString(R.string.pref_skipPicsHavingThumbnail_title)), Toast.LENGTH_LONG);
                    t.show();
                }
            }
        }

        public void addPixymetaToLibraryList() {
            CharSequence[] ent_def = getResources().getTextArray(R.array.exif_library_entries);
            CharSequence[] ent_pixymeta = getResources().getTextArray(R.array.exif_library_pixymeta_entries);
            CharSequence[] ent_all = new CharSequence[ent_def.length + ent_pixymeta.length];
            System.arraycopy(ent_def, 0, ent_all, 0, ent_def.length);
            System.arraycopy(ent_pixymeta, 0, ent_all, ent_def.length, ent_pixymeta.length);

            CharSequence[] val_def = getResources().getTextArray(R.array.exif_library_values);
            CharSequence[] val_pixymeta = getResources().getTextArray(R.array.exif_library_pixymeta_values);
            CharSequence[] val_all = new CharSequence[val_def.length + val_pixymeta.length];
            System.arraycopy(val_def, 0, val_all, 0, val_def.length);
            System.arraycopy(val_pixymeta, 0, val_all, val_def.length, val_pixymeta.length);

            ListPreference exif_libraryPref = findPreference("exif_library");
            exif_libraryPref.setEntries(ent_all);
            exif_libraryPref.setEntryValues(val_all);
        }

        public void updatePathsSummary() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
            Preference pathButtonPref = findPreference("selectPathButton");
            pathButtonPref.setSummary(inputDirs.toStringForDisplay(getContext()));
        }

        public void setKeepTimeStampOnBackupEnabled() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            boolean writeThumbnailedToOriginalFolder = prefs.getBoolean("writeThumbnailedToOriginalFolder", true);
            Preference keepTimeStampOnBackupPref = findPreference("keepTimeStampOnBackup");

            if (writeThumbnailedToOriginalFolder) {
                keepTimeStampOnBackupPref.setEnabled(false);
            } else {
                keepTimeStampOnBackupPref.setEnabled(true);
            }
        }

        private void setAllFilesAccess() {
            SwitchPreferenceCompat allFilesAccess = findPreference("settings_allFilesAccess");
            if (allFilesAccess == null) return;

            PreferenceCategory mCategory = (PreferenceCategory) findPreference("categ_Folders");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !BuildConfig.FLAVOR.equals("google_play")) {
                // update preference value
                allFilesAccess.setChecked(SettingsActivity.haveAllFilesAccessPermission());
            } else {
                // remove preference from the settings screen
                mCategory.removePreference(allFilesAccess);
            }
        }

        @TargetApi(30)
        private void requestAllFilesAccessPermission() {
            boolean intentFailed = false;
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            try {
                ComponentName componentName = intent.resolveActivity(getContext().getPackageManager());
                if (componentName != null) {
                    String className = componentName.getClassName();
                    if (className != null) {
                        // Launch "Allow all files access?" dialog.
                        startActivity(intent);
                        return;
                    }
                    intentFailed = true;
                } else {
                    if (enableLog) Log.w(TAG, "Request all files access not supported");
                    intentFailed = true;
                }
            } catch (ActivityNotFoundException e) {
                if (enableLog) Log.w(TAG, "Request all files access not supported", e);
                intentFailed = true;
            }
            if (intentFailed) {
                // Some devices don't support this request.
                Toast.makeText(this.getContext(), R.string.settings_dialog_all_files_access_not_supported, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == OPEN_DOCUMENT_TREE_RESULT_CODE) {
            Uri treeUri = resultData.getData();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION );
            setChosenPath( pickedDir.getUri());
        }
    }

    public void choosePaths(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        settingsFragment.setDoNotUnregisterPreferenceChangedListener(true);
        startActivityForResult(intent, OPEN_DOCUMENT_TREE_RESULT_CODE);
    }

    public void deletePaths(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // 1. Suppress the persistent permissions.
        InputDirs inputDirs = new InputDirs(prefs.getString("srcUris", ""));
        for (int i = 0; i< inputDirs.size(); i++) {
            releasePersistableUriPermission(inputDirs.get(i));
        }

        // 1. Remove the paths from the preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("srcUris", "");
        //editor.commit();
        editor.apply();
    }

    public void releasePersistableUriPermission(Uri uri) {
        uri = UriUtil.getAsTreeUri(uri);
        List<UriPermission> uriPermissionList = getContentResolver().getPersistedUriPermissions();

        for (int j = 0; j < uriPermissionList.size(); j++) {
            Uri permUri = uriPermissionList.get(j).getUri();
            if (permUri.equals(uri)) {
                getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
    }

    private void setChosenPath(Uri path) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefsUri = prefs.getString("srcUris", "");
        InputDirs inputDirs = new InputDirs(prefsUri);
        inputDirs.add(path);

        //prefs.edit().putString("srcUris", inputDirs.toString()).commit();
        prefs.edit().putString("srcUris", inputDirs.toString()).apply();
    }

    @TargetApi(30)
    public static boolean haveAllFilesAccessPermission() {
        return Environment.isExternalStorageManager();
    }
}
