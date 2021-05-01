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

import android.graphics.Bitmap;

import java.io.InputStream;
import java.io.OutputStream;

public class PixymetaInterface {
    public static boolean hasPixymetaLib() {
        return false;
    }
    public static void writeThumbnailWithPixymeta (
            InputStream srcImgIs, OutputStream newImgOs, Bitmap thumbnail)
            throws Exception {
        throw new PixymetaUnavailableException();
    }
    public static class PixymetaUnavailableException extends Exception {}
}
