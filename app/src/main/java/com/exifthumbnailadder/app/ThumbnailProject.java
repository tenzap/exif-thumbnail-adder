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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;

import com.exifthumbnailadder.app.exception.BadOriginalImageException;
import com.schokoladenbrown.Smooth;

class ThumbnailProject implements Parcelable {
    Bitmap original;
    int imageWidth;
    int imageHeight;
    Size targetSize;
    Smooth.Algo algo;
    Smooth.AlgoParametrized1 algo1;
    Smooth.AlgoParametrized2 algo2;
    double p0, p1;

    ThumbnailProject(Bitmap orig) throws Exception {
        setOriginal(orig);
    }

    ThumbnailProject(Bitmap orig, Smooth.Algo algo) throws Exception {
        this(orig);
        setAlgo(algo);
    }

    ThumbnailProject(Bitmap orig, Smooth.AlgoParametrized1 algo1, Double p0) throws Exception {
        this(orig);
        setAlgo1(algo1, p0);
    }

    ThumbnailProject(Bitmap orig, Smooth.AlgoParametrized2 algo2, Double p0, Double p1) throws Exception {
        this(orig);
        setAlgo2(algo2, p0, p1);
    }

    private void setOriginal(Bitmap orig) throws Exception {
        original = orig;
        if (original == null) {
            throw new BadOriginalImageException();
        }
        imageWidth = original.getWidth();
        imageHeight = original.getHeight();

        targetSize = ThumbnailFactory.getThumbnailTargetSize(imageWidth, imageHeight, 160);
    }

    public void setAlgo(Smooth.Algo algo) {
        this.algo = algo;
        this.algo1 = null;
        this.algo2 = null;
    }

    public void setAlgo1(Smooth.AlgoParametrized1 algo1, Double p0) {
        this.algo = null;
        this.algo1 = algo1;
        this.algo2 = null;
        this.p0 = p0;
    }

    public void setAlgo2(Smooth.AlgoParametrized2 algo2, Double p0, Double p1) {
        this.algo = null;
        this.algo1 = null;
        this.algo2 = algo2;
        this.p0 = p0;
        this.p1 = p1;
    }

    protected ThumbnailProject(Parcel in) {
        int tmpAlgo;
        original = in.readParcelable(Bitmap.class.getClassLoader());
        imageWidth = in.readInt();
        imageHeight = in.readInt();
        targetSize = in.readSize();
        tmpAlgo = in.readInt();
        algo = tmpAlgo == -1 ? null : Smooth.Algo.values()[tmpAlgo];
        tmpAlgo = in.readInt();
        algo1 = tmpAlgo == -1 ? null : Smooth.AlgoParametrized1.values()[tmpAlgo];
        p0 = in.readDouble();
        tmpAlgo = in.readInt();
        algo2 = tmpAlgo == -1 ? null : Smooth.AlgoParametrized2.values()[tmpAlgo];
        p1 = in.readDouble();
    }

    public static final Creator<ThumbnailProject> CREATOR = new Creator<ThumbnailProject>() {
        @Override
        public ThumbnailProject createFromParcel(Parcel in) {
            return new ThumbnailProject(in);
        }

        @Override
        public ThumbnailProject[] newArray(int size) {
            return new ThumbnailProject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(original, flags);
        dest.writeInt(imageWidth);
        dest.writeInt(imageHeight);
        dest.writeSize(targetSize);
        dest.writeInt(algo == null ? -1 : algo.ordinal());
        dest.writeInt(algo1 == null ? -1 : algo1.ordinal());
        dest.writeDouble(p0);
        dest.writeInt(algo2 == null ? -1 : algo2.ordinal());
        dest.writeDouble(p1);
    }
}
