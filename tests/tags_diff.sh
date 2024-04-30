#!/bin/bash

old_version=0.9.19
new_version=0.9.20

LIBS=(exiv2 aee libexif pixymeta)
TOOLS=(exiftool exiv2 identify-format identify-verbose)

APIS=()
while read -r line; do
  APIS+=("$(basename "$line")")
done < <(find "output/test_output-${new_version}/tags/exiv2" -maxdepth 1 -mindepth 1)

mydiff() {
  diff \
    -I '\[System\]        Directory.*' \
    -I '\[System\]        FileSize' \
    -I '^  Filename: .*' \
    -I '^    filename: ' \
    -x '*.err' \
    "$@"
}

for lib in "${LIBS[@]}"; do
  for api1 in "${APIS[@]}"; do
    for api2 in "${APIS[@]}"; do
      if [ "$api2" -ne $((api1 + 1)) ]; then continue; fi
      for tool in "${TOOLS[@]}"; do
        mydiff -Naur "output/test_output-${new_version}/tags/$lib/$api1/$tool/" "output/test_output-${new_version}/tags/$lib/$api2/$tool/" > "output/tags_diff/api_diff/${new_version}-$lib-$api1=${new_version}-$lib-$api2.$tool.diff"
      done
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api in "${APIS[@]}"; do
    if [ ! -d "output/test_output-${old_version}/tags/$lib/$api" ]; then continue; fi
    for tool in "${TOOLS[@]}"; do
      mydiff -Naur "output/test_output-${old_version}/tags/$lib/$api/$tool/" "output/test_output-${new_version}/tags/$lib/$api/$tool/" > "output/tags_diff/old_vs_new/${old_version}-$lib-$api=${new_version}-$lib-$api.$tool.diff"
    done
  done
done

for lib in "${LIBS[@]}"; do
  for api in "${APIS[@]}"; do
    for tool in "${TOOLS[@]}"; do
      mydiff -Naur "output/test_output-${new_version}/tags/original/$api/$tool/" "output/test_output-${new_version}/tags/$lib/$api/$tool/" > "output/tags_diff/original_vs_eta/0.9.19-original=${new_version}-$lib-$api.$tool.diff"
    done
  done
done
