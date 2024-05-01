#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if [ "$PWD/scripts" = "$SCRIPT_DIR" ]; then
    echo "Script is called from $PWD: OK!"
else
    echo
    echo "Must be run from project root dir: ./scripts/$( basename "${BASH_SOURCE[0]}" )"
    echo
    exit 1;
fi

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage:"
    echo "  ./scripts/$( basename "${BASH_SOURCE[0]}") <old_version> <new_version>"
    echo "  ./scripts/$( basename "${BASH_SOURCE[0]}") 0.9.19 0.9.20"
    echo
    exit 1
fi

export LC_ALL=C

# old_version=0.9.19
# new_version=0.9.20
old_version="$1"
new_version="$2"

LIBS=(exiv2 aee libexif pixymeta)
TOOLS=(exiftool exiv2 identify-format identify-verbose)

APIS_OLD=()
while read -r line; do
  APIS_OLD+=("$(basename "$line")")
done < <(find "tests/output/test_output-${old_version}/tags/exiv2" -maxdepth 1 -mindepth 1)

APIS_NEW=()
while read -r line; do
  APIS_NEW+=("$(basename "$line")")
done < <(find "tests/output/test_output-${new_version}/tags/exiv2" -maxdepth 1 -mindepth 1)

IGNORE_MATCHING_LINES=()
# For exiftool
IGNORE_MATCHING_LINES+=('\[System\]        Directory.*')
IGNORE_MATCHING_LINES+=('\[System\]        FileSize')
# For identify-verbose (Pixels per second, Elapsed time, date:create change on every run, so don't check them)
IGNORE_MATCHING_LINES+=('^  Filename: .*')
IGNORE_MATCHING_LINES+=('^    filename: ')
IGNORE_MATCHING_LINES+=('^  Filesize: ')
# Those may change on every run
IGNORE_MATCHING_LINES+=('^  Pixels per second: ')
IGNORE_MATCHING_LINES+=('^  User time: ')
IGNORE_MATCHING_LINES+=('^  Elapsed time: ')
IGNORE_MATCHING_LINES+=('^    date:create: ')
IGNORE_MATCHING_LINES+=('^    date:modify: ')
IGNORE_MATCHING_LINES+=('^Version: ImageMagick ')

DIFF_IGNORE_ARGS=()
for regex in "${IGNORE_MATCHING_LINES[@]}"; do
  DIFF_IGNORE_ARGS+=('-I')
  DIFF_IGNORE_ARGS+=("$regex")
done

mydiff() {
  diff \
    "${DIFF_IGNORE_ARGS[@]}" \
    -x '*.err' \
    "$@"
}

for lib in "${LIBS[@]}"; do
  for api1 in "${APIS_NEW[@]}"; do
    for api2 in "${APIS_NEW[@]}"; do
      if [ "$api2" -ne $((api1 + 1)) ]; then continue; fi
      for tool in "${TOOLS[@]}"; do
        mydiff -Naur "tests/output/test_output-${new_version}/tags/$lib/$api1/$tool/" "tests/output/test_output-${new_version}/tags/$lib/$api2/$tool/" > "tests/output/tags_diff/api_diff/${new_version}-$lib-$api1=${new_version}-$lib-$api2.$tool.diff"
      done
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api1 in "${APIS_OLD[@]}"; do
    for api2 in "${APIS_OLD[@]}"; do
      if [ "$api2" -ne $((api1 + 1)) ]; then continue; fi
      for tool in "${TOOLS[@]}"; do
        if [ -f "tests/output/tags_diff/api_diff/${old_version}-$lib-$api1=${old_version}-$lib-$api2.$tool.diff" ]; then continue; fi
        mydiff -Naur "tests/output/test_output-${old_version}/tags/$lib/$api1/$tool/" "tests/output/test_output-${old_version}/tags/$lib/$api2/$tool/" > "tests/output/tags_diff/api_diff/${old_version}-$lib-$api1=${old_version}-$lib-$api2.$tool.diff"
      done
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api in "${APIS_NEW[@]}"; do
    if [ ! -d "tests/output/test_output-${old_version}/tags/$lib/$api" ]; then continue; fi
    for tool in "${TOOLS[@]}"; do
      # Don't use -N to ignore new files
      mydiff -aur "tests/output/test_output-${old_version}/tags/$lib/$api/$tool/" "tests/output/test_output-${new_version}/tags/$lib/$api/$tool/" > "tests/output/tags_diff/old_vs_new/${old_version}-$lib-$api=${new_version}-$lib-$api.$tool.diff"
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api in "${APIS_OLD[@]}"; do
    for tool in "${TOOLS[@]}"; do
      if [ -f "tests/output/tags_diff/original_vs_eta/${old_version}-original=${old_version}-$lib-$api.$tool.diff" ]; then continue; fi
      mydiff -Naur "tests/output/test_output-${old_version}/tags/original/$api/$tool/" "tests/output/test_output-${old_version}/tags/$lib/$api/$tool/" > "tests/output/tags_diff/original_vs_eta/${old_version}-original=${old_version}-$lib-$api.$tool.diff"
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api in "${APIS_NEW[@]}"; do
    for tool in "${TOOLS[@]}"; do
      mydiff -Naur "tests/output/test_output-${new_version}/tags/original/$api/$tool/" "tests/output/test_output-${new_version}/tags/$lib/$api/$tool/" > "tests/output/tags_diff/original_vs_eta/${new_version}-original=${new_version}-$lib-$api.$tool.diff"
    done
  done
done
