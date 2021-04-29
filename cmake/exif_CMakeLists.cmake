cmake_minimum_required(VERSION 3.4.1)
project(exif LANGUAGES C)

set(PACKAGE "exif")

option(ENABLE_NLS "Enable NLS" OFF)
option(HAVE_LIBEXIF "Compile with libexif support" ON)
option(ENABLE_GETTEXT_ICONV "whether to run iconv on gettext output" OFF)

# PACKAGE_BUGREPORT needed by actions.c
add_definitions("-DPACKAGE_BUGREPORT=\"libexif-devel@lists.sourceforge.net\"")


################## libjpeg #################
set(SOURCES_libjpeg
        libjpeg/jpeg-data.c
        libjpeg/jpeg-data.h
        libjpeg/jpeg-marker.c
        libjpeg/jpeg-marker.h)

link_directories(${CMAKE_LIBRARY_OUTPUT_DIRECTORY})

add_library(jpeg STATIC ${SOURCES_libjpeg})

target_include_directories(jpeg PRIVATE .) # for "config.h"
target_include_directories(jpeg PRIVATE ${CMAKE_INSTALL_PREFIX}/include) # for libexif .h

target_link_libraries(jpeg exif)


################## exif #################
set(SOURCES_exif
        exif/actions.c
        exif/exif-i18n.c
        #exif/main.c # Don't compile main.c (it requires popt which we don't provide)
        exif/utils.c)

set(HEADERS_exif
        exif/actions.h
        exif/exif-i18n.h
        exif/utils.h)

link_directories(${CMAKE_LIBRARY_OUTPUT_DIRECTORY})

add_library(exif_app SHARED ${SOURCES_exif} ${HEADERS_exif})

target_include_directories(exif_app PRIVATE .) # for "config.h"
target_include_directories(exif_app PRIVATE ${CMAKE_INSTALL_PREFIX}/include) # for libexif .h

target_link_libraries(exif_app exif jpeg)


include(CheckFunctionExists)
include(CheckIncludeFile)

check_function_exists(dcgettext HAVE_DCGETTEXT)
check_function_exists(gettext HAVE_GETTEXT)
check_function_exists(iconv HAVE_ICONV)
check_function_exists(fileno HAVE_FILENO)
check_function_exists(mblen HAVE_MBLEN)

check_include_file(locale.h HAVE_LOCALE_H)
check_include_file(dlfcn.h HAVE_DLFCN_H)
check_include_file(inttypes.h HAVE_INTTYPES_H)
check_include_file(memory.h HAVE_MEMORY_H)
check_include_file(stdint.h HAVE_STDINT_H)
check_include_file(stdlib.h HAVE_STDLIB_H)
check_include_file(strings.h HAVE_STRINGS_H)
check_include_file(string.h HAVE_STRING_H)
check_include_file(sys/stat.h HAVE_SYS_STAT_H)
check_include_file(sys/types.h HAVE_SYS_TYPES_H)
check_include_file(unistd.h HAVE_UNISTD_H)

configure_file(config.h.cmake config.h)


install(
        TARGETS exif_app
        RUNTIME DESTINATION bin
        ARCHIVE DESTINATION lib
        LIBRARY DESTINATION lib
)

install(FILES ${HEADERS_exif}         DESTINATION include/libexif_app)
