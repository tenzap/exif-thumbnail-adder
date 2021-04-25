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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class InputDirs implements Serializable {
    private final List<Uri> selectedUris = new ArrayList<Uri>();
    JSONArray jsonArray = new JSONArray();

    InputDirs(String jsonArrayString) {
        try {
            if (! jsonArrayString.isEmpty()) {
                this.jsonArray = new JSONArray(jsonArrayString);
                for (int i=0; i<jsonArray.length(); i++) {
                    selectedUris.add(Uri.parse((String)jsonArray.get(i)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //if (enableLog) Log.i(TAG, "json: " + this.toString());
    }

    public void add(Uri uri) {
        if ( ! selectedUris.contains(uri)) {
            selectedUris.add(uri);
            jsonArray.put(uri);
        }
    }

    public Uri get(int index) {
        return selectedUris.get(index);
    }

    public int size() {
        return selectedUris.size();
    }

    public void delete(Uri uri) {
        selectedUris.remove(uri);
    }

    public String toString() {
        return jsonArray.toString();
    }

    public String toStringForDisplay(Context con) {
        StringBuilder output = new StringBuilder();
        for (int i=0; i<selectedUris.size(); i++) {
            output.append("⋅ " + FileUtil.getFullPathFromTreeUri(selectedUris.get(i), con));
            if (i != selectedUris.size()-1)
                output.append("\n");
        }
        return output.toString();
    }

    public Uri[] toUriArray() {
        Uri[] uris = new Uri[selectedUris.size()];
        uris = selectedUris.toArray(uris);
        return uris;
    }

    public File[] toFileArray(Context ctx) {
        File[] files = new File[selectedUris.size()];
        for (int i=0; i < selectedUris.size(); i++) {
            files[i] = new File(FileUtil.getFullPathFromTreeUri(selectedUris.get(i), ctx));
        }
        return files;
    }

}
