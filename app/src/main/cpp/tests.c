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
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ETA_nativeTest", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, "ETA_nativeTest", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ETA_nativeTest", __VA_ARGS__)

#include <stdio.h>
#include <string.h>
#include <errno.h>

#include "exceptions.h"

JNIEnv *jni_env_test;

static int read_file(const char* filename) {

    FILE* input_file = fopen(filename, "r");
    if (!input_file) {
        char msg[250] = {0, };
        snprintf(msg, 250, "Error reading file %s: %s\n", filename, strerror(errno));
        LOGE("%s", msg);
        throwNativeException(jni_env_test, msg);
        return 1;
    }

    fclose(input_file);

    return 0;

}

static int write_file(const char* filename) {

    FILE* output_file = fopen(filename, "rw");
    if (!output_file) {
        char msg[250] = {0, };
        snprintf(msg, 250, "Error writing file %s: %s\n", filename, strerror(errno));
        LOGE("%s", msg);
        throwNativeException(jni_env_test, msg);
        return 1;
    }

    fclose(output_file);

    return 0;

}

JNIEXPORT jint JNICALL Java_com_exifthumbnailadder_app_NativeLibHelper_readFile(
        JNIEnv * env,
        jobject thiz,
        jstring file
) {
    const char *inputFile;
    jni_env_test = env;
    inputFile = (*env)->GetStringUTFChars(env, file, NULL);
    return read_file(inputFile);
}

JNIEXPORT jint JNICALL Java_com_exifthumbnailadder_app_NativeLibHelper_writeFile(
        JNIEnv * env,
        jobject thiz,
        jstring file
) {
    const char *outputFile;
    jni_env_test = env;
    outputFile = (*env)->GetStringUTFChars(env, file, NULL);
    return write_file(outputFile);
}
