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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private final String homepage_url = "https://github.com/tenzap/exif-thumbnail-adder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }

    public void onStart(){
        super.onStart();

        String version = new String();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView txtview=(TextView)findViewById(R.id.textView_about_content);

        SpannableStringBuilder text = new SpannableStringBuilder();

        text.append("<p>");
        text.append("<u><strong>Version:</strong></u> " + version);
        text.append("</p><p>");
        text.append("<u><strong>Project homepage & source code:</strong></u><br><a href='"+homepage_url+"'>"+homepage_url+"</a>");
        text.append("</p><p>");
        text.append("</p><p>");
        text.append("<u><strong>License:</strong></u> <a href='http://www.gnu.org/licenses/gpl-3.0.html'>GNU General Public License, version 3</a>");
        text.append("</p><p>");
        text.append("<u><strong>External libraries</strong></u>");
        text.append("</p>");
        text.append("<ul><li>");
        text.append("<a href='https://github.com/sephiroth74/Android-Exif-Extended'>Android-Exif-Extended</a> Apache License, Version 2.0");
        text.append("</li><li>");
        text.append("<a href='https://developer.android.com/jetpack'>Android Jetpack</a> Apache License, Version 2.0");
        if (PixymetaInterface.hasPixymetaLib()) {
            text.append("</li><li>");
            text.append("<a href='https://github.com/dragon66/pixymeta-android'>pixymeta-android</a> Eclipse Public License - v 1.0");
        }
        text.append("</li></ul>");
        text.append("");

        //set text containing html tags to TextView
        txtview.setText(Html.fromHtml(text.toString(),Html.FROM_HTML_MODE_LEGACY));
        //make the link clickable
        txtview.setMovementMethod(LinkMovementMethod.getInstance());

    }
}