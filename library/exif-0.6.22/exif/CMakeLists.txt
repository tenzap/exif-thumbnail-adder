
set(exif_SRCS
    actions.h
    actions.c
    exif-i18n.h
    exif-i18n.c
    #main.c # Don't compile main.c (it requires popt which we don't provide)
    utils.h
    utils.c)

add_library(exifExecAsLib
        SHARED
        ${exif_SRCS}
    )

target_link_libraries(exifExecAsLib exif exifjpeg)

set(LIBEXIFDIR "../../libexif-0.6.22")
target_include_directories(exifExecAsLib PRIVATE ${LIBEXIFDIR})
