include(ExternalProject)

set (EXIF_VERSION 0.6.22)
set (EXIF_VERSION_PREBUILT 0.6.22)
set (EXIF_DIR exif-${EXIF_VERSION})

if(NOT USE_PREBUILT_LIB)
ExternalProject_Add(exif_external
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/${EXIF_DIR}
        #BUILD_IN_SOURCE 1
        CMAKE_ARGS
            ${CL_ARGS}
            -DCMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR}
            #-DCMAKE_INSTALL_LIBDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY} #Test

        DEPENDS libexif_external
        #BUILD_ALWAYS 1

        # fix for "missing and no known rule to make it": https://stackoverflow.com/a/65803911/15401262
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libexif_app.so

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
        LOG_UPDATE 1
        )

ExternalProject_Add_Step(
        exif_external
        add_cmake_files
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/cmake/exif_CMakeLists.cmake ${CMAKE_CURRENT_BINARY_DIR}/exif_external-prefix/src/exif_external/CMakeLists.txt
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/cmake/exif_config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/exif_external-prefix/src/exif_external/config.h.cmake
        DEPENDEES download
        DEPENDERS update
        LOG 1
)
endif()

add_library(exifLib SHARED IMPORTED)
if(USE_PREBUILT_LIB)
    set (EXIF_LIBRARY_SO_PATH ${CMAKE_SOURCE_DIR}/libs.prebuilt/exif-${EXIF_VERSION_PREBUILT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libexif_app.so)
else()
    add_dependencies(exifLib exif_external libexifLib)
    set (EXIF_LIBRARY_SO_PATH ${CMAKE_BINARY_DIR}/lib/libexif_app.so)
endif()
set_target_properties(
        # Specifies the target library.
        exifLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${EXIF_LIBRARY_SO_PATH}
)
