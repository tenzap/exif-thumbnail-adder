include(ExternalProject)

set (LIBEXIF_VERSION 0.6.24)
set (LIBEXIF_VERSION_PREBUILT 0.6.24)
set (LIBEXIF_DIR libexif-${LIBEXIF_VERSION})

if(NOT USE_PREBUILT_LIB)
ExternalProject_Add(libexif_external
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/${LIBEXIF_DIR}

        PATCH_COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/cmake/libexif_CMakeLists.cmake ${CMAKE_CURRENT_BINARY_DIR}/libexif_external-prefix/src/libexif_external/CMakeLists.txt
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/cmake/libexif_config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/libexif_external-prefix/src/libexif_external/config.h.cmake

        CMAKE_ARGS
            ${CL_ARGS}
            -DCMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR}
            #-DCMAKE_INSTALL_LIBDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY} #Test

        #DEPENDS
        #BUILD_ALWAYS 1 ## ETA addition: otherwise lib files might not be installed in the output dir

        # fix for "missing and no known rule to make it": https://stackoverflow.com/a/65803911/15401262
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libexif.so

        LOG_CONFIGURE 1
        LOG_UPDATE 1
#if(CMAKE_VERSION VERSION_GREATER_EQUAL "3.14")
#        LOG_PATCH 1
#endif()
        LOG_BUILD 1
        LOG_INSTALL 1
        )
endif()

add_library(libexifLib SHARED IMPORTED)
if(USE_PREBUILT_LIB)
    set (LIBEXIF_LIBRARY_SO_PATH ${CMAKE_SOURCE_DIR}/libs.prebuilt/libexif-${LIBEXIF_VERSION_PREBUILT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libexif.so)
else()
    add_dependencies(libexifLib libexif_external)
    set (LIBEXIF_LIBRARY_SO_PATH ${CMAKE_BINARY_DIR}/lib/libexif.so)
endif()
set_target_properties(
        # Specifies the target library.
        libexifLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${LIBEXIF_LIBRARY_SO_PATH}
    )
