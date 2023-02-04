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
import android.util.Log;

import com.exifthumbnailadder.app.exception.Exiv2ErrorException;
import com.exifthumbnailadder.app.exception.Exiv2WarnException;
import com.exifthumbnailadder.app.exception.LibexifException;
import com.exifthumbnailadder.app.exception.TimestampHelperException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static com.exifthumbnailadder.app.MainApplication.enableLog;
import static com.exifthumbnailadder.app.MainApplication.TAG;

public class NativeLibHelper {

    native byte[] writeThumbnailWithLibexif(
            byte[] srcImgBa, int srcNumBytes,
            byte[] newImgBa, int newNumBytes,
            byte[] thubmnail, int ntbNumBytes);

    native int writeThumbnailWithLibexifThroughFile(
            String in, String out, int width, int height, String tb, int resolution) throws Exception;

    native int writeThumbnailWithExiv2ThroughFile(
            String out, int width, int height, String tb, int resolution) throws Exception;

    native static int copyTimestamp(String file, String reference) throws TimestampHelperException;
    native static int setTimestamp(String file, double atime, double mtime) throws TimestampHelperException;

    native static int readFile(String file) throws TimestampHelperException;
    native static int writeFile(String file) throws TimestampHelperException;

    public void writeThumbnailWithLibexif (
            InputStream srcImgIs, OutputStream newImgOs, Bitmap thumbnail)
            throws Exception {
        // TODO
        // writeThumbnailWithLibexif
    }

    public void writeThumbnailWithLibexifThroughFile (String input, String output, int width, int height, Bitmap thumbnail, boolean libexifSkipOnError)
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
            int result = writeThumbnailWithLibexifThroughFile(input, output, width, height, tbFilename, 72);
            if (result != 0) throw new RuntimeException("libexif return value different from 0: " + result);
        } catch (LibexifException e) {
            if (libexifSkipOnError) {
                //Delete output file which might have been created by libexif despite the exception
                new File(output).delete();
            }
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

    public void writeThumbnailWithExiv2ThroughFile (String output, int width, int height, Bitmap thumbnail, String exiv2SkipOnLogLevel)
            throws Exception {
        String tbFilename = output+"_tb";
        File tbFile = new File(tbFilename);

        try {
            writeBitmapToFile(thumbnail, tbFile);
        } catch (Exception e) {
            if (enableLog) Log.e(TAG, "Exception with file: "+ tbFile);
            e.printStackTrace();
            throw e;
        }

        try {
            int result = writeThumbnailWithExiv2ThroughFile(output, width, height, tbFilename, 72);
            if (result != 0) throw new RuntimeException("exiv2 return value different from 0: " + result);
        } catch (Exiv2WarnException e) {
            switch (exiv2SkipOnLogLevel) {
                case "warn":
                    //Delete output file
                    new File(output).delete();
                    break;
            }
            throw e;
        } catch (Exiv2ErrorException e) {
            switch (exiv2SkipOnLogLevel) {
                case "warn":
                case "error":
                    //Delete output file
                    new File(output).delete();
                    break;
            }
            throw e;
        } catch (Exception e) {
            //Delete output file
            new File(output).delete();
            throw e;
        } finally {
            tbFile.delete();
        }
    }

    public void writeBitmapToFile(Bitmap thumbnail, File tbFile) throws Exception {
        tbFile.createNewFile();

        byte[] bitmapdata = bitmapToJPEGBytearray(thumbnail);

        //write the bytes in file
        FileOutputStream fos = new FileOutputStream(tbFile);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();
    }

    public static byte[] bitmapToJPEGBytearray(Bitmap thumbnail) {
        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapdata = bos.toByteArray();
        return bitmapdata;
    }
}
