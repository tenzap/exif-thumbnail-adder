# Adapted from:
# https://medium.com/@ilja.kosynkin/android-cmake-and-ffmpeg-part-two-building-ffmpeg-cd7f31f6c053
# https://github.com/IljaKosynkin/FFmpeg-Development-Kit/blob/cmake-ffmpeg-libx264/ffmpeg-sample/app/src/main/cpp/ffmpeg.cmake

cmake_minimum_required(VERSION 3.10.2)

include(ExternalProject)

# FFMPEG FETCH SECTION: START

set(FFMPEG_VERSION 4.4)
set(FFMPEG_VERSION_PREBUILT 4.4)
set(FFMPEG_NAME ffmpeg-${FFMPEG_VERSION})
set(FFMPEG_URL https://ffmpeg.org/releases/${FFMPEG_NAME}.tar.bz2)

set(ffmpegLibs
        libavutil
        libswscale
        #libavcodec
        #libavdevice
        #libavfilter
        #libavformat
        #libswresample
        )

# if CMAKE_HOST_WIN32 is true, then use precompiled library because we can't easily run "./configure" from win32 systems
if(NOT (USE_PREBUILT_LIB OR CMAKE_HOST_WIN32))
get_filename_component(FFMPEG_ARCHIVE_NAME ${FFMPEG_URL} NAME)

IF (NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME})
    file(DOWNLOAD ${FFMPEG_URL} ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_ARCHIVE_NAME})

    execute_process(
            COMMAND ${CMAKE_COMMAND} -E tar xzf ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_ARCHIVE_NAME}
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/library
    )

    # We're patching exit just before return in main function of ffmpeg.c because it will crash the application
    file(READ ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg.c ffmpeg_src)

    string(REPLACE "exit_program(received_nb_signals ? 255 : main_return_code);" "//exit_program(received_nb_signals ? 255 : main_return_code);" ffmpeg_src "${ffmpeg_src}")
    string(REPLACE "return main_return_code;" "return received_nb_signals ? 255 : main_return_code;" ffmpeg_src "${ffmpeg_src}")

    file(WRITE ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg.c "${ffmpeg_src}")
ENDIF()

foreach (lib IN LISTS ffmpegLibs)
    list(APPEND FFMPEG_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/${lib}.so)
endforeach()

#file(
#        COPY ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_build_system.cmake
#        DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}
#        FILE_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE
#)

# FFMPEG FETCH SECTION: END

# ANDROID BUILD TOOLS SECTION: START

# We set custom variables for all build tools in case we want to make changes to in certain situations
set(FFMPEG_CC ${CMAKE_C_COMPILER})
set(FFMPEG_CXX ${CMAKE_CXX_COMPILER})
set(FFMPEG_AR ${ANDROID_AR})
set(FFMPEG_AS ${ANDROID_ASM_COMPILER})

# Taken from https://android.googlesource.com/platform/ndk/+/master/build/cmake/android.toolchain.cmake
# Remove when toolchain actually will include it
set(FFMPEG_RANLIB ${ANDROID_TOOLCHAIN_PREFIX}ranlib${ANDROID_TOOLCHAIN_SUFFIX})
set(FFMPEG_STRIP ${ANDROID_TOOLCHAIN_ROOT}/bin/llvm-strip${ANDROID_TOOLCHAIN_SUFFIX})

# Set NM manually
set(FFMPEG_NM ${ANDROID_TOOLCHAIN_PREFIX}nm${ANDROID_TOOLCHAIN_SUFFIX})

# ANDROID BUILD TOOLS SECTION: END

# ANDROID FLAGS SECTION: START

# We remove --fatal-warnings in such blatant way because somebody at Google decided it's an excellent idea
# to put it into LD flags in toolchain without any way to turn it off.
string(REPLACE " -Wl,--fatal-warnings" "" FFMPEG_LD_FLAGS ${CMAKE_SHARED_LINKER_FLAGS})

set(FFMPEG_C_FLAGS "${CMAKE_C_FLAGS} --target=${ANDROID_LLVM_TRIPLE} --gcc-toolchain=${ANDROID_TOOLCHAIN_ROOT} ${FFMPEG_EXTRA_C_FLAGS}")
set(FFMPEG_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=${ANDROID_LLVM_TRIPLE} ${FFMPEG_EXTRA_ASM_FLAGS}")
set(FFMPEG_LD_FLAGS "${FFMPEG_C_FLAGS} ${FFMPEG_LD_FLAGS} ${FFMPEG_EXTRA_LD_FLAGS}")

# ANDROID FLAGS SECTION: END

# MISC VARIABLES SECTION: START

set(NJOBS 4)
set(HOST_BIN ${ANDROID_NDK}/prebuilt/${ANDROID_HOST_TAG}/bin)

# MISC VARIABLES SECTION: END

# FFMPEG EXTERNAL PROJECT CONFIG SECTION: START

# https://trac.ffmpeg.org/ticket/4928 we must disable asm for x86 since it's non-PIC by design from FFmpeg side
IF (${CMAKE_ANDROID_ARCH_ABI} STREQUAL x86)
    list(APPEND FFMPEG_CONFIGURE_EXTRAS --disable-asm)
ENDIF()

string(REPLACE ";" "|" FFMPEG_CONFIGURE_EXTRAS_ENCODED "${FFMPEG_CONFIGURE_EXTRAS}")
ExternalProject_Add(ffmpeg_target
        PREFIX ffmpeg_pref
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}
        DOWNLOAD_NO_EXTRACT 1
        CONFIGURE_COMMAND ${CMAKE_COMMAND} -E env
        PATH=${ANDROID_TOOLCHAIN_ROOT}/bin:$ENV{PATH}
        AS_FLAGS=${FFMPEG_ASM_FLAGS}
        ${CMAKE_COMMAND}
        -DSTEP:STRING=configure
        -DARCH:STRING=${CMAKE_SYSTEM_PROCESSOR}
        -DCC:STRING=${FFMPEG_CC}
        -DSTRIP:STRING=${FFMPEG_STRIP}
        -DAR:STRING=${FFMPEG_AR}
        -DAS:STRING=${FFMPEG_AS}
        -DNM:STRING=${FFMPEG_NM}
        -DRANLIB:STRING=${FFMPEG_RANLIB}
        -DSYSROOT:STRING=${CMAKE_SYSROOT}
        -DC_FLAGS:STRING=${FFMPEG_C_FLAGS}
        -DLD_FLAGS:STRING=${FFMPEG_LD_FLAGS}
        -DPREFIX:STRING=${CMAKE_CURRENT_BINARY_DIR}
        #-DINSTALL_LIBDIR:STRING=${CMAKE_LIBRARY_OUTPUT_DIRECTORY} # Test
        -DCONFIGURE_EXTRAS:STRING=${FFMPEG_CONFIGURE_EXTRAS_ENCODED}
        -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_build_system.cmake
        BUILD_COMMAND ${CMAKE_COMMAND} -E env
        PATH=${ANDROID_TOOLCHAIN_ROOT}/bin:$ENV{PATH}
        ${CMAKE_COMMAND}
        -DSTEP:STRING=build
        -NJOBS:STRING=${NJOBS}
        -DHOST_TOOLCHAIN:STRING=${HOST_BIN}
        -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_build_system.cmake
        BUILD_IN_SOURCE 1
        INSTALL_COMMAND ${CMAKE_COMMAND} -E env
        PATH=${ANDROID_TOOLCHAIN_ROOT}/bin:$ENV{PATH}
        ${CMAKE_COMMAND}
        -DSTEP:STRING=install
        -DHOST_TOOLCHAIN:STRING=${HOST_BIN}
        -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_build_system.cmake
        STEP_TARGETS copy_headers
        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
        DEPENDS ${FFMPEG_DEPENDS}

        #BUILD_ALWAYS 1

        PATCH_COMMAND patch -p1 -i ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_Makefile.patch  ## ETA addition: because we removed some dirs from ffmpeg sources
        #LOG_PATCH 1 ## ETA addition: needs CMAKE >= version 3.14

        # fix for "missing and no known rule to make it": https://stackoverflow.com/a/65803911/15401262
        BUILD_BYPRODUCTS ${FFMPEG_BYPRODUCTS}
        )

ExternalProject_Get_property(ffmpeg_target SOURCE_DIR)
#ExternalProject_Add_Step(
#        ffmpeg_target
#        copy_headers
#        COMMAND ${CMAKE_COMMAND}
#        -DBUILD_DIR:STRING=${SOURCE_DIR}
#        -DSOURCE_DIR:STRING=${CMAKE_CURRENT_SOURCE_DIR}
#        -DFFMPEG_NAME:STRING=${FFMPEG_NAME}
#        -DOUT:STRING=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
#        -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/ffmpeg_copy_headers.cmake
#        DEPENDEES build
#        DEPENDERS install
#)

# FFMPEG EXTERNAL PROJECT CONFIG SECTION: END

# FFMPEG EXE SOURCES SECTION: START

set(ffmpeg_src
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/cmdutils.c
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg_cuvid.c
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg_filter.c
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg_hw.c
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg_opt.c
        ${CMAKE_CURRENT_SOURCE_DIR}/library/${FFMPEG_NAME}/fftools/ffmpeg.c
        )

# FFMPEG EXE SOURCES SECTION: END
endif() # end of "if USE_PREBUILT_LIB"


foreach (lib IN LISTS ffmpegLibs)
    add_library(${lib}Lib SHARED IMPORTED)
    if(USE_PREBUILT_LIB OR CMAKE_HOST_WIN32)
        set (${lib}_LIBRARY_SO_PATH ${CMAKE_SOURCE_DIR}/libs.prebuilt/ffmpeg-${FFMPEG_VERSION_PREBUILT}/lib/${CMAKE_ANDROID_ARCH_ABI}/${lib}.so)
    else()
        add_dependencies(${lib}Lib ffmpeg_target)
        set (${lib}_LIBRARY_SO_PATH ${CMAKE_BINARY_DIR}/lib/${lib}.so)
    endif()
    set_target_properties(
            # Specifies the target library.
            ${lib}Lib
            # Specifies the parameter you want to define.
            PROPERTIES IMPORTED_LOCATION
            # Provides the path to the library you want to import.
            ${${lib}_LIBRARY_SO_PATH}
    )
endforeach()
