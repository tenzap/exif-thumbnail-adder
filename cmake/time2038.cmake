# Copyright (C) 2025 SciVision, Inc.
# SPDX-License-Identifier: MIT
# Adapted from: https://gist.github.com/scivision/f582e5c24a3efbf414a7557dfe99a2fb

cmake_minimum_required(VERSION 3.20)

include(CheckSourceRuns)
include(CheckTypeSize)

check_type_size("time_t" TIME_T_SIZE)
message("size of time_t: ${TIME_T_SIZE} (stored in TIME_T_SIZE)")

if(NOT TIME_T_SIZE GREATER_EQUAL 8)
  set(CMAKE_REQUIRED_DEFINITIONS -D_TIME_BITS=64)
  message(STATUS "Trying ${CMAKE_REQUIRED_DEFINITIONS} to get 64-bit time_t")
  check_type_size("time_t" TIME_T_SIZE_64)
  message("size of time_t with ${CMAKE_REQUIRED_DEFINITIONS}: ${TIME_T_SIZE_64} (stored in TIME_T_SIZE_64)")

  if(TIME_T_SIZE_64 EQUAL 0)
    message("multiple architectures detected.")
  elseif(TIME_T_SIZE_64 LESS 8)
    message(WARNING "time_t is not 64-bit")
  endif()

  if(NOT TIME_T_SIZE_64 LESS 8)
    set(_TIME_BITS 64)
    message("Need definition ${CMAKE_REQUIRED_DEFINITIONS} to use 64-bit time_t")
  endif()
endif()

file(READ ${CMAKE_CURRENT_SOURCE_DIR}/time2038.c src)

# Cannot make the check because we are cross compiling
#check_source_runs(C "${src}" OK2038)
#
#if(NOT OK2038)
#  message(FATAL_ERROR "time_t is not 64-bit")
#endif()
