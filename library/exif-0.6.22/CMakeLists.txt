cmake_minimum_required(VERSION 3.4.1)
project(exifExecAsLib LANGUAGES C)

# PACKAGE_BUGREPORT needed by actions.c
add_definitions("-DPACKAGE_BUGREPORT=\"libexif-devel@lists.sourceforge.net\"")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)

add_subdirectory(libjpeg)
add_subdirectory(exif)

target_include_directories(exifExecAsLib PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_include_directories(exifExecAsLib PRIVATE .)
