#!/bin/bash

# SPDX-FileCopyrightText: 2024 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

#
# This requires to be able to mount and unmount with sudo.
# You may need to add these commands to sudo config file
#
# /usr/bin/mount -t tmpfs -o size=1500M tmpfs *exif-thumbnail-adder*
# /usr/bin/mount -t overlay *exif-thumbnail-adder*
# /usr/bin/umount *exif-thumbnail-adder*
#
# Invoke with:
#
#   ./extract_tags.sh "github_artifact.zip"

# To create a zip suitable as input for this script:
#  tar -cvf artifact.tar tests/output/test_output tests/output/test_pics/
#  zip artifact.zip artifact.tar
# or single command:
#  tar -cf - tests/output/test_output tests/output/test_pics/ | zip artifact.zip -
#

set -e

zipfile="$1"
overlay_dir="$(realpath "$(dirname "$1")")/overlay"

tmpfs="$overlay_dir/zip_content"
upper="$overlay_dir/../tags"
work="$overlay_dir/work"
merged="$overlay_dir/merged"

mkdir -p "$tmpfs" "$upper" "$work" "$merged"

echo
echo "Input file: $zipfile"
echo

if ! findmnt "$tmpfs"; then
  echo "Mouting tmpfs to ${tmpfs/$PWD\//}"
  sudo mount -t tmpfs -o size=1500M tmpfs "$tmpfs"
fi

# shellcheck disable=SC2012
if [ "$(ls -1 "$tmpfs" | wc -l)" = "0" ]; then
  echo "Extracting files from $1 to ${tmpfs/$PWD\//}"
  funzip "$zipfile" | tar -x -C "$tmpfs" --strip-components=2 --wildcards \
    tests/output/test_pics \
    tests/output/*/standard_addThumbsSettingsAEE/ \
    tests/output/*/standard_addThumbsSettingsLibExif/ \
    tests/output/*/standard_addThumbsSettingsPixymeta/ \
    tests/output/*/standard_addThumbsSettingsExiv2/

fi

if ! findmnt "$merged"; then
  echo "Mouting overlay to ${merged/$PWD\//}"
  # https://wiki.archlinux.org/title/Overlay_filesystem
  sudo mount -t overlay overlay -o lowerdir="$tmpfs",upperdir="$upper",workdir="$work" "$merged"
fi

api="$(ls "$tmpfs/test_output")"

while read -r file; do
  #echo "$file"
  echo -n "."
  case "$file" in
  */DCIM/test_standard_addThumbsSettingsAEE/*)
    lib=aee
    # shellcheck disable=SC2001
    path="$(echo "$file" | sed -e "s|.*/test_standard_addThumbsSettingsAEE/\(.*\)|\1|")"
    ;;
  */DCIM/test_standard_addThumbsSettingsLibExif/*)
    lib=libexif
    # shellcheck disable=SC2001
    path="$(echo "$file" | sed -e "s|.*/test_standard_addThumbsSettingsLibExif/\(.*\)|\1|")"
    ;;
  */DCIM/test_standard_addThumbsSettingsPixymeta/*)
    lib=pixymeta
    # shellcheck disable=SC2001
    path="$(echo "$file" | sed -e "s|.*/test_standard_addThumbsSettingsPixymeta/\(.*\)|\1|")"
    ;;
  */DCIM/test_standard_addThumbsSettingsExiv2/*)
    lib=exiv2
    # shellcheck disable=SC2001
    path="$(echo "$file" | sed -e "s|.*/test_standard_addThumbsSettingsExiv2/\(.*\)|\1|")"
    ;;
  */test_pics/*)
    lib=original
    # shellcheck disable=SC2001
    path="$(echo "$file" | sed -e "s|.*/test_pics/\(.*\)|\1|")"
    ;;
  esac

  if [ -n "$lib" ]; then
    output_filename_exiftool="$merged/${lib}/${api}/exiftool/$path"
    output_filename_identify_verbose="$merged/${lib}/${api}/identify-verbose/$path"
    output_filename_identify_format="$merged/${lib}/${api}/identify-format/$path"
    output_filename_exiv2="$merged/${lib}/${api}/exiv2/$path"
    mkdir -p "$(dirname "${output_filename_exiftool}")"  "$(dirname "${output_filename_identify_verbose}")"  "$(dirname "${output_filename_identify_format}")"  "$(dirname "${output_filename_exiv2}")"
    exiftool -a -G1 -sort -s -e --Filename --FileAccessDate --FileInodeChangeDate --FileModifyDate --FileCreateDate "$file" 2> "${output_filename_exiftool}.err" > "${output_filename_exiftool}" || echo "Processing error..." > "${output_filename_exiftool}.failed"

    identify -verbose "$file" 2> "${output_filename_identify_verbose}.err" > "${output_filename_identify_verbose}" || echo "Processing error..." > "${output_filename_identify_verbose}.failed"

    identify -format '%[EXIF:*]' "$file" 2> "${output_filename_identify_format}.err" > "${output_filename_identify_format}" || echo "Processing error..." > "${output_filename_identify_format}.failed"

    exiv2_for_diff.sh "$file" 2> "${output_filename_exiv2}.err" > "${output_filename_exiv2}" || echo "Processing error..." > "${output_filename_exiv2}.failed"
  fi
  unset lib
done < <(find "$merged/test_"* -type f)

sudo umount "$merged"
sudo umount "$tmpfs"
rm -r "${tmpfs:?}" "${merged:?}"
rm -rf "${work:?}"

echo "Done."
