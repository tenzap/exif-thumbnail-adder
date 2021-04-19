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

#include "Exiv2Helper.h"

#include <iostream>

#include <exiv2/exiv2.hpp>

int Exiv2Helper::insertThumbnail(
        const std::string &path,
        const std::string &thumbPath,
        Exiv2::URational xres,
        Exiv2::URational yres,
        uint16_t unit) const {
    try {
        if (!Exiv2::fileExists(thumbPath, true)) {
            throw Exiv2::Error(Exiv2::kerErrorMessage, "exiv2: Failed to open the file: " + thumbPath);
        }
        if (!Exiv2::fileExists(path, true)) {
            throw Exiv2::Error(Exiv2::kerErrorMessage, "exiv2: Failed to open the file: " + path);
        }

        Exiv2::Image::AutoPtr image = Exiv2::ImageFactory::open(path);
        assert(image.get() != 0);
        image->readMetadata();
        Exiv2::ExifThumb exifThumb(image->exifData());

        // Erase previous thumbnail tags
        exifThumb.erase();

        // Add new thumbnail
        exifThumb.setJpegThumbnail(thumbPath, xres, yres, unit);
        image->writeMetadata();

        // If there was at least 1 error in the process, throw exception that we will catch later in Java.
        if (!errorMsg.empty())
            throw Exiv2::Error(Exiv2::kerErrorMessage, "exiv2: " + errorMsg);

    } catch (const std::exception& exc) {
        throw;
    }
    return 0;
}

void Exiv2Helper::errorHandler(int level, const char* s)
{
    std::string message;
    switch (static_cast<Exiv2::LogMsg::Level>(level)) {
        case Exiv2::LogMsg::debug: message = "Debug: "; break;
        case Exiv2::LogMsg::info:  message =  "Info: "; break;
        case Exiv2::LogMsg::warn:  message =  "Warning: "; break;
        case Exiv2::LogMsg::error: message =  "Error: "; break;
        case Exiv2::LogMsg::mute:  assert(false);
    }
    This->errorMsg = message + s + " " + This->errorMsg;
    return;
}

//https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
jint Exiv2Helper::throwNoClassDefError( JNIEnv *env, const char *message ) const
{
    jclass exClass;
    const char *className = "java/lang/NoClassDefFoundError";

    exClass = (env)->FindClass( const_cast<char *>(className));
    if (exClass == NULL) {
        return throwNoClassDefError( env, const_cast<char *>(className) );
    }

    return env->ThrowNew( exClass, message );
}

//https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
jint Exiv2Helper::throwError( JNIEnv *env, const char *message ) const
{
    if ((env)->ExceptionCheck() == JNI_TRUE) {
        return 99;
    }

    jclass exClass;
    const char *className = "java/lang/RuntimeException" ;

    exClass = (env)->FindClass( const_cast<char *>(className) );
    if ( exClass == NULL ) {
        return throwNoClassDefError( env, const_cast<char *>(className) );
    }

    return (env)->ThrowNew( exClass, message );
}

jint Exiv2Helper::throwExiv2ErrorException( JNIEnv *env, const char *message ) const
{
    if ((env)->ExceptionCheck() == JNI_TRUE) {
        return 99;
    }

    jclass exClass;
    const char *className = "com/exifthumbnailadder/app/Exiv2ErrorException" ;

    exClass = (env)->FindClass( const_cast<char *>(className) );
    if ( exClass == NULL ) {
        return throwNoClassDefError( env, const_cast<char *>(className) );
    }

    return (env)->ThrowNew( exClass, message );
}

jint Exiv2Helper::throwExiv2WarnException( JNIEnv *env, const char *message ) const
{
    if ((env)->ExceptionCheck() == JNI_TRUE) {
        return 99;
    }

    jclass exClass;
    const char *className = "com/exifthumbnailadder/app/Exiv2WarnException" ;

    exClass = (env)->FindClass( const_cast<char *>(className) );
    if ( exClass == NULL ) {
        return throwNoClassDefError( env, const_cast<char *>(className) );
    }

    return (env)->ThrowNew( exClass, message );
}

// https://stackoverflow.com/a/59312811/15401262
// Initialize variable Exiv2Helper::This
Exiv2Helper* Exiv2Helper::This = nullptr; // or NULL or even 0

extern "C" JNIEXPORT jint JNICALL Java_com_exifthumbnailadder_app_NativeLibHelper_writeThumbnailWithExiv2ThroughFile(
        JNIEnv *env,
        jobject /* this */,
        jstring joutput,
        jstring jtb,
        jint resolution) {

    jboolean isCopy;
    const char *filePath = (env)->GetStringUTFChars(joutput, &isCopy);
    const char *tbPath = (env)->GetStringUTFChars(jtb, &isCopy);
    Exiv2::URational res = Exiv2::URational(resolution, 1);
    int resUnit = 2; // 2 means "inches"
    int rc = 0;

    Exiv2Helper helper;

    try {
        Exiv2::XmpParser::initialize();
        ::atexit(Exiv2::XmpParser::terminate);

        Exiv2::LogMsg::setLevel(Exiv2::LogMsg::warn);
        Exiv2::LogMsg::setHandler(Exiv2Helper::errorHandler);

        int ret = helper.insertThumbnail(
                filePath,
                tbPath,
                res,
                res,
                resUnit);
        if (rc == 0)
            rc = ret;

        Exiv2::XmpParser::terminate();

    } catch (const std::exception& exc) {
        std::cerr << "Uncaught exception: " << exc.what() << std::endl;
        if (std::strstr(exc.what(),"Error: ") != NULL) {
            // The exception contains the string "Error: ". We throw "throwExiv2ErrorException"
            helper.throwExiv2ErrorException(env, exc.what());
        } else {
            helper.throwExiv2WarnException(env, exc.what());
        }
        rc = 1;
    }

    // Return a positive one byte code for better consistency across platforms
    //return static_cast<unsigned int>(rc) % 256;
    return rc;
}
