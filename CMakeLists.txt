cmake_minimum_required(VERSION 3.4.1)

include(ExternalProject)

# Make a copy of all CMAKE arguments for use in subproject
# Needed so that the exteral project have all the config for android (ninja, android toolchain...)
# see: https://stackoverflow.com/a/48555098/15401262
# MUST be done before call to 'project'
get_cmake_property(vars CACHE_VARIABLES)
foreach(var ${vars})
    get_property(currentHelpString CACHE "${var}" PROPERTY HELPSTRING)
    if("${currentHelpString}" MATCHES "No help, variable specified on the command line." OR "${currentHelpString}" STREQUAL "")
        # message("${var} = [${${var}}]  --  ${currentHelpString}") # uncomment to see the variables being processed
        list(APPEND CL_ARGS "-D${var}=${${var}}")
    endif()
endforeach()

project(SuperBuild)

##############################################
# Include external projects/libraries
##############################################

#### libexif
include(cmake/libexif.cmake)

#### exif
include(cmake/exif.cmake)

#### Expat (required by xmpsdk in exiv2)
include(cmake/expat.cmake)

#### exiv2
include(cmake/exiv2.cmake)

#### ffmpeg
set(FFMPEG_CONFIGURE_EXTRAS --enable-jni --enable-mediacodec)
include(cmake/ffmpeg.cmake)

#### Now to build the native-lib code specific to "Exif Thumbnail Adder"
add_subdirectory(
        app/src/main/cpp
)
