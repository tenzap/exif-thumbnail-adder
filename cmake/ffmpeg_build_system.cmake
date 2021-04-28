# Adapted from:
# https://medium.com/@ilja.kosynkin/android-cmake-and-ffmpeg-part-two-building-ffmpeg-cd7f31f6c053
# https://github.com/IljaKosynkin/FFmpeg-Development-Kit/blob/cmake-ffmpeg-libx264/ffmpeg-sample/app/src/main/cpp/ffmpeg_build_system.cmake

cmake_minimum_required(VERSION 3.10.2)

if (${STEP} STREQUAL configure)
    # Encoding string to list
    string(REPLACE "|" ";" CONFIGURE_EXTRAS_ENCODED "${CONFIGURE_EXTRAS}")
    list(REMOVE_ITEM CONFIGURE_EXTRAS_ENCODED "")

    # Note that we don't pass LD, Clang sets it internally based of --target
    set(CONFIGURE_COMMAND
            ./configure
            --cc=${CC}
            --ar=${AR}
            --strip=${STRIP}
            --ranlib=${RANLIB}
            --as=${AS}
            --nm=${NM}
            --target-os=android
            --arch=${ARCH}
            --extra-cflags=${C_FLAGS}
            --extra-ldflags=${LD_FLAGS}
            --sysroot=${SYSROOT}
            --enable-cross-compile
            --disable-static
            --disable-programs
            --disable-doc
            --enable-shared
            --enable-protocol=file
            --enable-pic
            --shlibdir=${PREFIX}
            --prefix=${PREFIX}

            # In ExifThumbnailAdder we need only swscale, so disable all other
            --disable-avdevice
            --disable-avcodec
            --disable-avformat
            --disable-swresample
            #--disable-swscale
            --disable-postproc
            --disable-avfilter

            ${CONFIGURE_EXTRAS_ENCODED}
    )

    execute_process(COMMAND ${CONFIGURE_COMMAND})
elseif(${STEP} STREQUAL build)
    execute_process(COMMAND ${HOST_TOOLCHAIN}/make all -j${NJOBS})
elseif(${STEP} STREQUAL install)
    execute_process(COMMAND ${HOST_TOOLCHAIN}/make install)
endif()
