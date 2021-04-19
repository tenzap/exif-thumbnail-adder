cmake_minimum_required(VERSION 3.4.1)
project(exifThumbnailAdderHelpers LANGUAGES C CXX)

set(PACKAGE "exifThumbnailAdderHelpers")

set(LIBEXIFDIR  "../../../../library/libexif-0.6.22")
set(EXIFDIR     "../../../../library/exif-0.6.22")
set(EXIV2DIR    "../../../../library/exiv2-0.27.3-Source")

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

add_library(exifThumbnailAdderHelpers
        SHARED
        ${SOURCES} ${HEADERS}
        ${SOURCES_Exiv2Helper} ${HEADERS_Exiv2Helper}
        ${exiv2}
        )

target_include_directories(exifThumbnailAdderHelpers PRIVATE .)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_CURRENT_BINARY_DIR})

add_dependencies(exifThumbnailAdderHelpers exif exifExecAsLib)
target_link_libraries(exifThumbnailAdderHelpers exif exifExecAsLib)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${LIBEXIFDIR})
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR})
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR}/exif)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIFDIR}/libjpeg)

add_dependencies(exifThumbnailAdderHelpers exiv2lib)
target_link_libraries(exifThumbnailAdderHelpers exiv2lib)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIV2DIR}/src)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${EXIV2DIR}/include)
target_include_directories(exifThumbnailAdderHelpers PRIVATE ${CMAKE_BINARY_DIR}) # for "exiv2lib_export.h"
