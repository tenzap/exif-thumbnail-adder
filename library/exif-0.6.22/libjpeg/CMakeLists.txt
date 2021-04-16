
set(jpeg_STAT_SRCS
    jpeg-data.c
    jpeg-data.h
    jpeg-marker.c
    jpeg-marker.h)

add_library(exifjpeg SHARED ${jpeg_STAT_SRCS})

target_link_libraries(exifjpeg exif)

target_include_directories(exifjpeg PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_include_directories(exifjpeg PRIVATE .)
target_include_directories(exifjpeg PRIVATE ..)
set(LIBEXIFDIR "../../libexif-0.6.22")
target_include_directories(exifjpeg PRIVATE ${LIBEXIFDIR})
