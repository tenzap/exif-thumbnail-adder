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

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.util.Size;

import com.schokoladenbrown.Smooth;

class ThumbnailFactory {

    public static Size getThumbnailTargetSize(int imageWidth, int imageHeight, int maxSize) {
        float imageRatio = ((float) Math.min(imageWidth, imageHeight) / (float) Math.max(imageWidth, imageHeight));
        int thumbnailWidth = (imageWidth < imageHeight) ? Math.round(maxSize * imageRatio) : maxSize;
        int thumbnailHeight = (imageWidth < imageHeight) ? maxSize : Math.round(maxSize * imageRatio);
//        if (imageWidth < imageHeight) {
//            // Swap thumbnail width and height to keep a relative aspect ratio
//            int temp = thumbnailWidth;
//            thumbnailWidth = thumbnailHeight;
//            thumbnailHeight = temp;
//        }
        if (imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
        if (imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;

        return new Size(thumbnailWidth, thumbnailHeight);
    }

    ThumbnailFactory() {
    }

    public Bitmap getThumbnailWithFfmpegService(ThumbnailProject tp) throws Exception {
        //return Smooth.rescale(tp.original, tp.targetSize.getWidth(), tp.targetSize.getHeight(), Smooth.Algo.BILINEAR);
        //return Smooth.rescale(tp.original, tp.targetSize.getWidth(), tp.targetSize.getHeight(), Smooth.Algo.SINC);
        return Smooth.rescale(tp.original, tp.targetSize.getWidth(), tp.targetSize.getHeight(), Smooth.AlgoParametrized1.LANCZOS, 3.0);  // 3 is default width in ffmpeg.
    }

    public Bitmap getThumbnailWithThumbnailUtils(ThumbnailProject tp) throws Exception {
        int tmpWidth, tmpHeight;
        Bitmap thumbnail = null;

        tmpWidth = tp.imageWidth;
        tmpHeight = tp.imageHeight;

        thumbnail = tp.original;
        while (tmpWidth / tp.targetSize.getWidth() > 2 || tmpHeight / tp.targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, tmpWidth, tmpHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return ThumbnailUtils.extractThumbnail(thumbnail, tp.targetSize.getWidth(), tp.targetSize.getHeight(), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
    }

    public Bitmap getThumbnailWithCreateScaledBitmap(ThumbnailProject tp) throws Exception {
        int tmpWidth, tmpHeight;
        Bitmap thumbnail = null;

        tmpWidth = tp.imageWidth;
        tmpHeight = tp.imageHeight;
        thumbnail = tp.original;
        while (tmpWidth / tp.targetSize.getWidth() > 2 || tmpHeight / tp.targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = Bitmap.createScaledBitmap(thumbnail, tmpWidth, tmpHeight, true);
        }
        return Bitmap.createScaledBitmap(thumbnail, tp.targetSize.getWidth(), tp.targetSize.getHeight(), true);
    }
}
