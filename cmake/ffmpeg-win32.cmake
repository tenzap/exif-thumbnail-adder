cmake_minimum_required(VERSION 3.10.2)

include(ExternalProject)

add_library(libavutilLib SHARED IMPORTED)
#add_dependencies(libavutilLib ffmpeg_target)
set_target_properties(
        # Specifies the target library.
        libavutilLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/libs.prebuilt/ffmpeg-4.4/lib/${CMAKE_ANDROID_ARCH_ABI}/libavutil.so
)

add_library(libswscaleLib SHARED IMPORTED)
#add_dependencies(libswscaleLib ffmpeg_target)
set_target_properties(
        # Specifies the target library.
        libswscaleLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/libs.prebuilt/ffmpeg-4.4/lib/${CMAKE_ANDROID_ARCH_ABI}/libswscale.so
)
