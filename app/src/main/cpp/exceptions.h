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

#include <jni.h>

#ifndef EXIF_THUMBNAIL_ADDER_EXCEPTIONS_H
#define EXIF_THUMBNAIL_ADDER_EXCEPTIONS_H

jint throwNoClassDefError( JNIEnv *env, char *message );
jint throwError( JNIEnv *env, char *message );
jint throwNativeException( JNIEnv *env, char *message );

#endif //EXIF_THUMBNAIL_ADDER_EXCEPTIONS_H
