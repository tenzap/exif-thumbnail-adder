include(ExternalProject)

set (EXIV2_VERSION 0.27.5)
set (EXIV2_VERSION_PREBUILT 0.27.5)
set (EXIV2_DIR exiv2-${EXIV2_VERSION})
set (EXIV2_SOURCE_DIRNAME ${EXIV2_DIR}-Source)

if(NOT USE_PREBUILT_LIB)
#set(CMAKE_CXX_STANDARD 11)             # Uncommenting this line leads to "no stop on breakpoints in C++ code in Android Studio"
#set(CMAKE_CXX_FLAGS -Wno-deprecated)   # Uncommenting this line leads to "no stop on breakpoints in C++ code in Android Studio"
ExternalProject_Add(exiv2_external
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/${EXIV2_SOURCE_DIRNAME}
        CMAKE_ARGS
            ${CL_ARGS}                                         # Copies the cmake arguments (all parameters for android) to the package
            -DCMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR} # Sets dir where cmake will install the files of the package
            -DCMAKE_FIND_ROOT_PATH=${CMAKE_CURRENT_BINARY_DIR} # So that cmake's find_package finds EXPAT
            -DEXIV2_BUILD_SAMPLES=OFF
            -DEXIV2_ENABLE_PNG=OFF
            -DEXIV2_BUILD_SAMPLES=OFF
            -DEXIV2_BUILD_EXIV2_COMMAND=OFF
            #-DCMAKE_INSTALL_LIBDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY} #Test

        DEPENDS expat_external
        #BUILD_ALWAYS 1

        # fix for "missing and no known rule to make it": https://stackoverflow.com/a/65803911/15401262
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libexiv2.so

        LOG_CONFIGURE 1
        LOG_BUILD 1
        LOG_INSTALL 1
        LOG_UPDATE 1
        )
endif()

add_library(libexiv2Lib SHARED IMPORTED)
if(USE_PREBUILT_LIB)
    set (EXIV2_LIBRARY_SO_PATH ${CMAKE_SOURCE_DIR}/libs.prebuilt/exiv2-${EXIV2_VERSION_PREBUILT}/lib/${CMAKE_ANDROID_ARCH_ABI}/libexiv2.so)
else()
    add_dependencies(libexiv2Lib exiv2_external)
    set (EXIV2_LIBRARY_SO_PATH ${CMAKE_BINARY_DIR}/lib/libexiv2.so)
endif()
add_dependencies(libexiv2Lib libexpatLib)
set_target_properties(
        # Specifies the target library.
        libexiv2Lib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${EXIV2_LIBRARY_SO_PATH}
)
