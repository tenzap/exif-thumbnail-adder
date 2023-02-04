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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Link the BottomNavigationView with the navigation graph
        // https://developer.android.com/guide/navigation/navigation-ui#bottom_navigation
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        NavHostFragment navHostFragment = (NavHostFragment)fragmentManager.findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        bottomNavigationView.setItemHorizontalTranslationEnabled(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // In case the useSAF preference was changed with a debug release,
        // here we revert it to the default value "enabled"
        if (!BuildConfig.BUILD_TYPE.equals("debug")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("useSAF", true);
            //editor.commit();
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            //return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        displayFragmentOfLastService();
    }

    private void displayFragmentOfLastService() {
        // Move to the fragment that ran the last service so that user can see the logs of that service.
        String lastExecutedService = LastServiceLiveData.get().getValue();
        if (lastExecutedService != null && !lastExecutedService.isEmpty()) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().getPrimaryNavigationFragment();
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                Fragment foregroundFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                if (lastExecutedService.equals("com.exifthumbnailadder.app.AddThumbsService")) {
                    if (foregroundFragment instanceof SyncFragment) {
                        navController.navigate(R.id.action_SyncFragment_to_AddThumbsFragment);
                    } else if (foregroundFragment instanceof SettingsFragment) {
                        navController.navigate(R.id.action_SettingsFragment_to_AddThumbsFragment);
                    }
                    // reset lastService because we have brought the user to the correct fragment
                    // of the last service that has run. Now navigation is again in the hands of the user.
                    if (!ServiceUtil.isServiceRunning(this, SyncService.class) &&
                            !ServiceUtil.isServiceRunning(this, AddThumbsService.class)) {
                        LastServiceLiveData.get().setLastService("");
                    }
                } else if (lastExecutedService.equals("com.exifthumbnailadder.app.SyncService")) {
                    if (foregroundFragment instanceof AddThumbsFragment) {
                        navController.navigate(R.id.action_AddThumbsFragment_to_SyncFragment);
                    } else if (foregroundFragment instanceof SettingsFragment) {
                        navController.navigate(R.id.action_SettingsFragment_to_SyncFragment);
                    }
                    // reset lastService because we have brought the user to the correct fragment
                    // of the last service that has run. Now navigation is again in the hands of the user.
                    if (!ServiceUtil.isServiceRunning(this, SyncService.class) &&
                            !ServiceUtil.isServiceRunning(this, AddThumbsService.class)) {
                        LastServiceLiveData.get().setLastService("");
                    }
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
/*
        String k = key.toString();
        Toast t = null;
        switch(k) {
            case "exif_library":
            case "selectPathButton":
                t = Toast.makeText(this, key + ": " + sharedPreferences.getString(k,"???"), Toast.LENGTH_SHORT);
                t.show();
                break;
            case "writeTmpToCacheDir":
            case "writeThumbnailedToOriginalFolder":
            case "rotateThumbnails":
                t = Toast.makeText(this, key + ": " + sharedPreferences.getBoolean(k, false), Toast.LENGTH_SHORT);
                t.show();
                break;
        }
*/
    }

    ActivityResultLauncher<Intent> mLauncherOpenDocumentTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri treeUri = result.getData().getData();
                        DocumentFile pickedDir = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
                        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION );
                        setChosenPath( pickedDir.getUri());
                    }
                }
            });

    public Fragment getForegroundFragment(){
        Fragment navHostFragment = getSupportFragmentManager().getPrimaryNavigationFragment();
        return navHostFragment == null ? null : navHostFragment.getChildFragmentManager().getFragments().get(0);
    }

    public void choosePaths(View view) {
        Fragment currentFragment = getForegroundFragment();
        if (currentFragment instanceof SettingsFragment) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            ((SettingsFragment) currentFragment).setDoNotUnregisterPreferenceChangedListener(true);
            mLauncherOpenDocumentTree.launch(intent);
        }
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

        prefs.edit().putString("srcUris", inputDirs.toString()).commit();
    }

}
