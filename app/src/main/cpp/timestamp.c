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

#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ETA_timestamp", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, "ETA_timestamp", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ETA_timestamp", __VA_ARGS__)

#include <sys/stat.h>
#include <stdio.h>
#include <time.h>
#include <utime.h>
#include <string.h>
#include <errno.h>
#include "exceptions.h"

JNIEnv *jni_env;

jint throwTimestampHelperException( JNIEnv *env, char *message )
{
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        return 99;
    }

    jclass exClass;
    char *className = "com/exifthumbnailadder/app/exception/TimestampHelperException" ;

    exClass = (*env)->FindClass( env, className );
    if ( exClass == NULL ) {
        return throwNoClassDefError( env, className );
    }

    return (*env)->ThrowNew( env, exClass, message );
}

int touch_with_ref(const char* file, const char* file_ref) {
    struct stat ref;
    time_t atime;
    time_t mtime;
    struct utimbuf new_times;

    if (stat(file_ref, &ref) < 0) {
        char msg[250] = {0, };
        snprintf(msg, 250, "Error reading timestamp: %s: %s\n", file, strerror(errno));
        LOGE("%s", msg);
        throwTimestampHelperException(jni_env, msg);
//        perror(file_ref);
        return 1;
    }

    atime = ref.st_atime; /* seconds since the epoch */
    mtime = ref.st_mtime; /* seconds since the epoch */

    new_times.actime = ref.st_atime; /* keep atime unchanged */
    new_times.modtime = ref.st_mtime;    /* set mtime to current time */
    if (utime(file, &new_times) < 0) {
        char msg[250] = {0, };
        snprintf(msg, 250, "Error writing timestamp: %s: %s\n", file, strerror(errno));
        LOGE("%s", msg);
        throwTimestampHelperException(jni_env, msg);
//        perror(file);
        return 1;
    }

    return 0;
}

int touch(const char* file, double d_atime, double d_mtime) {
    time_t atime;
    time_t mtime;
    struct utimbuf new_times;

    atime = d_atime; /* seconds since the epoch */
    mtime = d_mtime; /* seconds since the epoch */

    new_times.actime = atime;
    new_times.modtime = mtime;
    if (utime(file, &new_times) < 0) {
        char msg[250] = {0, };
        snprintf(msg, 250, "Error setting timestamp: %s: %s\n", file, strerror(errno));
        LOGE("%s", msg);
        throwTimestampHelperException(jni_env, msg);
//        perror(file);
        return 1;
    }

    return 0;
}

int main_timestamp() {
    const char *file = "input.txt";
    const char *file_ref = "ref_file.txt";
    return touch_with_ref(file, file_ref);
}

JNIEXPORT jint JNICALL Java_com_exifthumbnailadder_app_NativeLibHelper_copyTimestamp(
        JNIEnv * env,
        jobject thiz,
        jstring file,
        jstring reference
) {
    const char *inputFile, *refFile;
    jni_env = env;
    inputFile = (*env)->GetStringUTFChars(env, file, NULL);
    refFile = (*env)->GetStringUTFChars(env, reference, NULL);
    return touch_with_ref(inputFile, refFile);
}

JNIEXPORT jint JNICALL Java_com_exifthumbnailadder_app_NativeLibHelper_setTimestamp(
        JNIEnv * env,
        jobject thiz,
        jstring file,
        jdouble atime,
        jdouble mtime
) {
    const char *inputFile;
    jni_env = env;
    jdouble local_atime, local_mtime;
    inputFile = (*env)->GetStringUTFChars(env, file, NULL);
    return touch(inputFile, atime, mtime);
}
