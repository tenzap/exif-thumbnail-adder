include(ExternalProject)

ExternalProject_Add(expat_external
        URL ${CMAKE_CURRENT_SOURCE_DIR}/library/expat-2.3.0
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

add_library(libexpatLib SHARED IMPORTED)
add_dependencies(libexpatLib expat_external)
set_target_properties(
        # Specifies the target library.
        libexpatLib
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${CMAKE_BINARY_DIR}/lib/libexpat.so
)