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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class NativeLibHelper {

    native byte[] writeThumbnailWithLibexif(
            byte[] srcImgBa, int srcNumBytes,
            byte[] newImgBa, int newNumBytes,
            byte[] thubmnail, int ntbNumBytes);

    native int writeThumbnailWithLibexifThroughFile(
            String in, String out, String tb) throws Exception;

    native int writeThumbnailWithExiv2ThroughFile(
            String out, String tb, int resolution) throws Exception;

    public void writeThumbnailWithLibexif (
            InputStream srcImgIs, OutputStream newImgOs, Bitmap thumbnail)
            throws Exception {
        // TODO
        // writeThumbnailWithLibexif
    }

    public void writeThumbnailWithLibexifThroughFile (String input, String output, Bitmap thumbnail)
            throws Exception {
        String tbFilename = output+"_tb";
        File tbFile = new File(tbFilename);

        try {
            writeBitmapToFile(thumbnail, tbFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        try {
            int result = writeThumbnailWithLibexifThroughFile(input, output, tbFilename);
            if (result != 0) throw new RuntimeException("libexif return value different from 0: " + result);
        } catch (Exception e) {
            //Delete output file which might have been created by libexif despite the exception
            new File(output).delete();
            e.printStackTrace();
            throw e;
        } finally {
            tbFile.delete();
        }
    }

    public void writeThumbnailWithExiv2ThroughFile (String output, Bitmap thumbnail, String exiv2SkipOnLogLevel)
            throws Exception {
        String tbFilename = output+"_tb";
        File tbFile = new File(tbFilename);

        try {
            writeBitmapToFile(thumbnail, tbFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        try {
            int result = writeThumbnailWithExiv2ThroughFile(output, tbFilename, 72);
            if (result != 0) throw new RuntimeException("exiv2 return value different from 0: " + result);
        } catch (Exiv2WarnException e) {
            if (exiv2SkipOnLogLevel.equals("warn")) {
                //Delete output file which might have been created by libexif despite the exception
                new File(output).delete();
            }
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            //Delete output file which might have been created by libexif despite the exception
            new File(output).delete();
            e.printStackTrace();
            throw e;
        } finally {
            tbFile.delete();
        }
    }

    public void writeBitmapToFile(Bitmap thumbnail, File tbFile) throws Exception {
        tbFile.createNewFile();

        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in file
        FileOutputStream fos = new FileOutputStream(tbFile);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();
    }

}
