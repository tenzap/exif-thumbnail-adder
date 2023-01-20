/* SPDX-License-Identifier: (BSD-2-Clause or GPL-2.0-only) */
// Adapted for Exif Thumbnail Adder
// From: https://raw.githubusercontent.com/ser-gik/smoothrescale/master/smoothrescale/src/main/jni/on_load.c

#include <inttypes.h>
#include <signal.h>
#include <stddef.h>
#include <stdint.h>

#include <jni.h>

#include <android/log.h>
#include <android/bitmap.h>

#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
#include <libavutil/log.h>

static jboolean enableLog;
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "schokoladenbrown", __VA_ARGS__)

struct bitmap {
    jobject           jbitmap;
    AndroidBitmapInfo info;
    uint8_t           *buffer;
};

static int lock_bitmap(JNIEnv *env, struct bitmap *bm) {
    int res = AndroidBitmap_getInfo(env, bm->jbitmap, &bm->info);
    if(ANDROID_BITMAP_RESULT_SUCCESS != res) return res;
    else return AndroidBitmap_lockPixels(env, bm->jbitmap, (void **)&bm->buffer);
}

static int unlock_bitmap(JNIEnv *env, struct bitmap *bm) {
    const static struct bitmap null_bm; 
    int res = AndroidBitmap_unlockPixels(env, bm->jbitmap);
    *bm = null_bm;
    return res;
}

static inline enum AVPixelFormat pix_fmt(enum AndroidBitmapFormat fmt) {
    /* bitmap formats directly correspond to SkColorType values */
    switch (fmt) {
        case ANDROID_BITMAP_FORMAT_RGBA_8888:
            /*
             * kN32_SkColorType
             * Actually it may be one of kBGRA_8888_SkColorType or kRGBA_8888_SkColorType
             * and may be configured at build time. Seems like Android uses RGBA order.
             */
            return AV_PIX_FMT_RGBA;;
        case ANDROID_BITMAP_FORMAT_RGB_565:
            /*
             * kRGB_565_SkColorType
             * This one is packed in native endianness
             */
            return AV_PIX_FMT_RGB565;
        case ANDROID_BITMAP_FORMAT_A_8:
            /*
             * kAlpha_8_SkColorType
             * There is no appropriate AV_PIX_FMT_*
             */
            /* fall through */
        default:
            return AV_PIX_FMT_NONE;
    }
}

static void store_to_file(struct bitmap *bmp, const char* name) {

    FILE* pFile;

    // "/data/local/tmp" is apparently not writable, so use another path.
    char filePath[100] = "/storage/emulated/0/DCIM/test_pics/";
    strcat(filePath, name);

    const uint8_t *src_planes[] = { bmp->buffer };

    pFile = fopen(filePath,"wb");
    if (pFile) {
        // Write buffer to disk.
        fwrite(*src_planes, 4, bmp->info.width * bmp->info.height, pFile);
        LOGI("Wrote to file!");
    } else {
        LOGI("Something wrong writing to File.");
    }
    fclose(pFile);
}

static jobject JNICALL native_rescale_impl(JNIEnv *env, jclass clazz,
        jobject srcBitmap, jobject dstBitmap, jint sws_algo, jdouble p0, jdouble p1) {
    struct bitmap src = { .jbitmap = srcBitmap };
    struct bitmap dst = { .jbitmap = dstBitmap };
    jobject ret = NULL;

    if (enableLog) {
        LOGI("algo %x %lf %lf", sws_algo, p0, p1);
    }
    if(ANDROID_BITMAP_RESULT_SUCCESS == lock_bitmap(env, &src)
            && ANDROID_BITMAP_RESULT_SUCCESS == lock_bitmap(env, &dst)) {
        const uint8_t *src_planes[] = { src.buffer };
        const int src_strides[] = { src.info.stride };
        uint8_t *dst_planes[] = { dst.buffer };
        const int dst_strides[] = { dst.info.stride };
        const double params[] = { p0, p1 };
        struct SwsContext *ctx;

        if (enableLog) {
            LOGI("[src]: flags: %"PRIu32", format: %"PRIi32", height: %"PRIu32", stride: %"PRIu32", width: %"PRIu32,
                 src.info.flags,
                 src.info.format,
                 src.info.height,
                 src.info.stride,
                 src.info.width);
            LOGI("[dst]: flags: %"PRIu32", format: %"PRIi32", height: %"PRIu32", stride: %"PRIu32", width: %"PRIu32,
                 dst.info.flags,
                 dst.info.format,
                 dst.info.height,
                 dst.info.stride,
                 dst.info.width);

            // This may require 'All files access' depending on where we write the files
            //store_to_file(&src, "src");
            //store_to_file(&dst, "dst");

            // enable SWS_PRINT_INFO
            sws_algo |= SWS_PRINT_INFO;
            LOGI("sws_algo (with SWS_PRINT_INFO): %i", sws_algo);

            // Set ffmpeg's loglevel to max (=AV_LOG_TRACE)
            av_log_set_flags(AV_LOG_PRINT_LEVEL | AV_LOG_SKIP_REPEATED);
            av_log_set_level(AV_LOG_TRACE);
        }

        ctx = sws_getContext(src.info.width, src.info.height, pix_fmt(src.info.format),
                             dst.info.width, dst.info.height, pix_fmt(dst.info.format),
                             sws_algo, NULL, NULL, params);
        if(ctx) {
            int res = sws_scale(ctx, src_planes, src_strides, 0, src.info.height, dst_planes, dst_strides);
            sws_freeContext(ctx);
            if(res > 0) {
                ret = dstBitmap;
            }
        }
    }

    unlock_bitmap(env, &src);
    unlock_bitmap(env, &dst);
    return ret;
}

static int JNICALL request_crash_impl(JNIEnv *env, jclass clazz,
                                      jobject srcBitmap, jobject dstBitmap, jint sws_algo, jdouble p0, jdouble p1) {

    raise(SIGSEGV);
    return 200;
}

static const char *g_rescaler_java_class = "com/schokoladenbrown/Smooth";
static const JNINativeMethod g_native_methods[] = {
    {"native_rescale",
     "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IDD)Landroid/graphics/Bitmap;",
     native_rescale_impl},
    {"request_crash",
     "()I",
     request_crash_impl},
};

jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = NULL;
    jclass cls;

    (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    cls = (*env)->FindClass(env, g_rescaler_java_class);
    (*env)->RegisterNatives(env, cls, g_native_methods, sizeof g_native_methods / sizeof g_native_methods[0]);

    // get enableLog value from MainApplication.enableLog
    jclass clsMainApplication = (*env)->FindClass(env,
                                                  "com/exifthumbnailadder/app/MainApplication");
    jfieldID enableLogFieldId = (*env)->GetStaticFieldID(env, clsMainApplication, "enableLog", "Z");
    enableLog = (*env)->GetStaticBooleanField(env, clsMainApplication, enableLogFieldId);

    return JNI_VERSION_1_2;
}
