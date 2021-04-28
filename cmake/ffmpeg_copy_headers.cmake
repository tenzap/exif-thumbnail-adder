# Adapted from:
# https://medium.com/@ilja.kosynkin/android-cmake-and-ffmpeg-part-two-building-ffmpeg-cd7f31f6c053
# https://github.com/IljaKosynkin/FFmpeg-Development-Kit/blob/cmake-ffmpeg-libx264/ffmpeg-sample/app/src/main/cpp/copy_headers.cmake

cmake_minimum_required(VERSION 3.10.2)

file(GLOB libs ${SOURCE_DIR}/${FFMPEG_NAME}/lib*)
file(
        COPY ${libs} ${BUILD_DIR}/config.h ${SOURCE_DIR}/${FFMPEG_NAME}/compat
        DESTINATION ${OUT}/include
        FILES_MATCHING PATTERN *.h
)
