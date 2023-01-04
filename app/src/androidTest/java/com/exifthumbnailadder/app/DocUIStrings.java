/*
 * Copyright (C) 2021-2023 Fab Stz <fabstz-it@yahoo.fr>
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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;

import java.util.List;

public class DocUIStrings {

    String allow = "";
    String allowAccessTo = "";
    String select = "";
    String save = "";
    String documentsUiPackageName = "";
    String showInternalStorage;
    String moreOptions;

    DocUIStrings() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        int resId;

        PackageManager manager = context.getPackageManager();
        List<PackageInfo> packagesList = manager.getInstalledPackages(0);
        for (PackageInfo pkg : packagesList) {
            if (pkg.packageName.equals("com.android.documentsui")) {
                documentsUiPackageName = "com.android.documentsui";
                break;
            } else if (pkg.packageName.equals("com.google.android.documentsui")) {
                documentsUiPackageName = "com.google.android.documentsui";
                break;
            }
        }
        if (documentsUiPackageName.isEmpty())
            throw new UnsupportedOperationException("Couldn't find 'DocumentsUi' package.");

        try {
            // Identifier names are taken here:
            // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r30:packages/apps/DocumentsUI/res/values/strings.xml
            Resources resources = manager.getResourcesForApplication(documentsUiPackageName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resId = resources.getIdentifier("allow", "string", documentsUiPackageName);
                allow = resources.getString(resId);
                resId = resources.getIdentifier("open_tree_button", "string", documentsUiPackageName);
                allowAccessTo = resources.getString(resId);
                allowAccessTo = allowAccessTo.replaceFirst(".%1\\$s..*", ""); //remove "%1$s"

            } else {
                resId = resources.getIdentifier("button_select", "string", documentsUiPackageName);
                select = resources.getString(resId);
            }
            resId = resources.getIdentifier("menu_save", "string", documentsUiPackageName);
            save = resources.getString(resId);

            resId = resources.getIdentifier("menu_advanced_show", "string", documentsUiPackageName);
            showInternalStorage = resources.getString(resId);

            resId = resources.getIdentifier("action_menu_overflow_description", "string", "android");
            moreOptions = resources.getString(resId);

        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getSave(){
        return save;
    }

    public String getSelect() {
        return select;
    }

    public String getAllow() {
        return allow;
    }

    public String getAllowAccessTo() {
        return allowAccessTo;
    }

    public String getDocumentsUiPackageName() {
        return documentsUiPackageName;
    }

    public String getShowInternalStorage() {
        return showInternalStorage;
    }

    public String getMoreOptions() {
        return moreOptions;
    }

}
