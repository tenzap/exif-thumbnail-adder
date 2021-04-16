cmake_minimum_required(VERSION 3.4.1)

# Adds the CMakeLists.txt file located in the specified directory
# as a build dependency.
add_subdirectory(
        library/libexif-0.6.22
)

add_subdirectory(
        library/exif-0.6.22
)
