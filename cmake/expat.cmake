include(ExternalProject)

set (EXPAT_VERSION 2.3.0)
set (EXPAT_VERSION_PREBUILT 2.3.0)
set (EXPAT_DIR expat-${EXPAT_VERSION})

if(NOT USE_PREBUILT_LIB)
ExternalProject_Add(expat_external
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/${EXPAT_DIR}
        CMAKE_ARGS
            ${CL_ARGS}
            -DCMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR}

        #DEPENDS
        #BUILD_ALWAYS 1

        # fix for "missing and no known rule to make it": https://stackoverflow.com/a/65803911/15401262
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libexpat.so

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
        LOG_UPDATE 1
        )
endif()

add_library(libexpatLib SHARED IMPORTED)
if(USE_PREBUILT_LIB)
    set (EXPAT_LIBRARY_SO_PATH ${CMAKE_SOURCE_DIR}/libs.prebuilt/expat-${EXPAT_VERSION_PREBUILT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libexpat.so)
else()
    add_dependencies(libexpatLib expat_external)
    set (EXPAT_LIBRARY_SO_PATH ${CMAKE_BINARY_DIR}/lib/libexpat.so)
endif()
set_target_properties(
        # Specifies the target library.
        libexpatLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${EXPAT_LIBRARY_SO_PATH}
)