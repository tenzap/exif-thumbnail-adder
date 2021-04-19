cmake_minimum_required(VERSION 3.4.1)

# Adds the CMakeLists.txt file located in the specified directory
# as a build dependency.
add_subdirectory(
        library/libexif-0.6.22
)

add_subdirectory(
        library/exif-0.6.22
)

add_subdirectory(
        library/expat-2.3.0
)

# For exiv2:
#set(CMAKE_CXX_STANDARD 11)             # Uncommenting this line leads to "no stop on breakpoints in C++ code in Android Studio"
#set(CMAKE_CXX_FLAGS -Wno-deprecated)   # Uncommenting this line leads to "no stop on breakpoints in C++ code in Android Studio"

# Overwrite exiv2 cmake options
set(EXIV2_BUILD_SAMPLES         OFF CACHE BOOL "Build sample applications"              FORCE)
set(EXIV2_ENABLE_PNG            OFF CACHE BOOL "Build with png support (requires libz)" FORCE)
set(EXIV2_BUILD_EXIV2_COMMAND   OFF CACHE BOOL "Build exiv2 command-line executable"    FORCE)

# Set EXPAT variables needed by exiv2/xmpsdk
set(EXPAT_LIBRARY expat)
set(EXPAT_INCLUDE_DIR ${PROJECT_SOURCE_DIR}/../expat-2.3.0/lib)
add_subdirectory(
        library/exiv2-0.27.3-Source
)

add_subdirectory(
        app/src/main/cpp
)
