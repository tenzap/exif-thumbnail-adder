cmake_minimum_required(VERSION 3.4.1)
project(exifThumbnailAdderHelpers LANGUAGES C)

set(PACKAGE "exifThumbnailAdderHelpers")

set(SOURCES
        libexif-helper.c
        )

set(HEADERS
        libexif-helper.h
        )

add_library(exifThumbnailAdderHelpers
        SHARED
        ${SOURCES}
        ${HEADERS}
        )

add_dependencies(exifThumbnailAdderHelpers exif exifExecAsLib)

target_link_libraries(exifThumbnailAdderHelpers exif exifExecAsLib)

target_include_directories(exifThumbnailAdderHelpers PRIVATE .)
#target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_BINARY_DIR}) # Use CMAKE_CURRENT_BINARY_DIR instead
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
set(LIBEXIFDIR "../../../../library/libexif-0.6.22")
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${LIBEXIFDIR})
set(EXIFDIR "../../../../library/exif-0.6.22")
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR})
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR}/exif)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR}/libjpeg)
