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

import pixy.image.tiff.IFD;
import pixy.image.tiff.RationalField;
import pixy.image.tiff.ShortField;
import pixy.image.tiff.TiffFieldEnum;
import pixy.image.tiff.TiffTag;
import pixy.meta.Metadata;
import pixy.meta.exif.ExifThumbnail;
import pixy.meta.exif.JpegExif;

public class PixymetaInterface {
    public static boolean hasPixymetaLib() {
        return true;
    }
    public static void writeThumbnailWithPixymeta (
            InputStream srcImgIs, OutputStream newImgOs, Bitmap thumbnail)
            throws Exception {
        // PixyMeta doesn't copy correctly the IFDInterop
        try {
            IFD tbIFD = new IFD();
            ExifThumbnail exifTb = new ExifThumbnail(thumbnail.getWidth(), thumbnail.getHeight(), ExifThumbnail.DATA_TYPE_KJpegRGB, FirstFragment.bitmapToJPEGBytearray(thumbnail), tbIFD);
            JpegExif jpegExif = new JpegExif();
            jpegExif.setThumbnail(exifTb);

            // set other mandatory tags for IFD1 (compression, resolution, res unit)
            tbIFD.addField(new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)TiffFieldEnum.Compression.OLD_JPG.getValue()}));
            tbIFD.addField(new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{2}));
            tbIFD.addField(new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[] {72,1}));
            tbIFD.addField(new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[] {72,1}));

            Metadata.insertExif(srcImgIs, newImgOs, jpegExif, true);
        } catch (Exception e) {
            throw e;
        }
    }
    public static class PixymetaUnavailableException extends Exception {}
}
