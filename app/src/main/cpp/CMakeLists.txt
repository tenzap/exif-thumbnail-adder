cmake_minimum_required(VERSION 3.4.1)
project(exifThumbnailAdderHelpers LANGUAGES C CXX)

set(PACKAGE "exifThumbnailAdderHelpers")

include(ExternalProject)

# Fetch source location of exiv2 and store in "EXIV2DIR"
ExternalProject_Get_Property(exiv2_external SOURCE_DIR)
set(EXIV2DIR ${SOURCE_DIR})
unset(SOURCE_DIR)

set(SOURCES
        libexif-helper.c
        )

set(HEADERS
        libexif-helper.h
        )

set(SOURCES_Exiv2Helper
        Exiv2Helper.cpp
        )

set(HEADERS_Exiv2Helper
        Exiv2Helper.h
        )

set (exiv2
        ${EXIV2DIR}/src/exiv2.cpp
        ${EXIV2DIR}/src/exiv2app.hpp
        ${EXIV2DIR}/src/actions.cpp ${EXIV2DIR}/src/actions.hpp
        ${EXIV2DIR}/src/getopt.cpp ${EXIV2DIR}/src/getopt.hpp
        ${EXIV2DIR}/src/utils.cpp ${EXIV2DIR}/src/utils.hpp
        )

# exifThumbnailAdderHelpers links to libavutil & libswscale
# we need to use link_directories so that target_link_libraries works.
# link_directories must be set before "add_library", otherwise the libs will not be found
#link_directories(${CMAKE_LIBRARY_OUTPUT_DIRECTORY})

add_library(exifThumbnailAdderHelpers
        SHARED
        ${SOURCES} ${HEADERS}
        ${SOURCES_Exiv2Helper} ${HEADERS_Exiv2Helper}
        ${exiv2}
        ffmpeg_helper.c
        )

target_include_directories(exifThumbnailAdderHelpers PRIVATE .)
#target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_BINARY_DIR}/include)

# for libexif_helper
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_BINARY_DIR}/include/libexif_app)
target_link_libraries(exifThumbnailAdderHelpers libexifLib exifLib)

# for Exiv2Helper
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_BINARY_DIR}/include/exiv2)
target_link_libraries(exifThumbnailAdderHelpers libexpatLib) # To be sure libexpat.so is added since it is needed by exiv2
target_link_libraries(exifThumbnailAdderHelpers libexiv2Lib)

# for ffmpeg_helper
target_link_libraries(exifThumbnailAdderHelpers libswscaleLib libavutilLib)
target_link_libraries(exifThumbnailAdderHelpers log)         # for __android_log_print
target_link_libraries(exifThumbnailAdderHelpers jnigraphics) # for android bitmap in jni/c
