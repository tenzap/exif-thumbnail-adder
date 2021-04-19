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

#include <jni.h>

#include <exiv2/exiv2.hpp>
#include <string>

#ifndef EXIF_THUMBNAIL_ADDER_EXIV2HELPER_H
#define EXIF_THUMBNAIL_ADDER_EXIV2HELPER_H

class Exiv2Helper {

public:
    Exiv2Helper() { This = this; }
    std::string errorMsg;

    static Exiv2Helper* This; // Here is our "this" pointer :P

    static void errorHandler(int level, const char* s);

    jint throwError( JNIEnv *env, const char *message ) const;
    jint throwNoClassDefError( JNIEnv *env, const char *message ) const;
    jint throwExiv2ErrorException( JNIEnv *env, const char *message ) const;
    jint throwExiv2WarnException( JNIEnv *env, const char *message ) const;

    int insertThumbnail(const std::string& path,
                        const std::string& thumbPath,
                        Exiv2::URational xres,
                        Exiv2::URational yres,
                        uint16_t unit) const;

};

#endif //EXIF_THUMBNAIL_ADDER_EXIV2HELPER_H
