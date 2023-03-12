/*
 * Copyright (C) 2023 Fab Stz <fabstz-it@yahoo.fr>
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
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class ETAExifInterface extends ExifInterface {

    public ETAExifInterface(@NonNull File file) throws IOException {
        super(file);
    }

    public ETAExifInterface(@NonNull String filename) throws IOException {
        super(filename);
    }

    public ETAExifInterface(@NonNull FileDescriptor fileDescriptor) throws IOException {
        super(fileDescriptor);
    }

    public ETAExifInterface(@NonNull InputStream inputStream) throws IOException {
        super(inputStream);
    }

    public ETAExifInterface(@NonNull InputStream inputStream, int streamType) throws IOException {
        super(inputStream, streamType);
    }

    public boolean hasXmpMetadataFromSeparateMarker() throws Exception {
        try {
            Field privateStringField = ExifInterface.class.getDeclaredField("mXmpIsFromSeparateMarker");
            privateStringField.setAccessible(true);
            boolean localXmpIsFromSeparateMarker = (boolean) privateStringField.get(this);
            return localXmpIsFromSeparateMarker;
        } catch (NoSuchFieldException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }
}
