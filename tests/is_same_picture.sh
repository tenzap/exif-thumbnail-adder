#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage:
#   is_same_picture pic1.jpg pic2.jpg

engine="findimagedupes_with_prior_resize"

ORIGINAL="$1"
THUMBNAIL="$2"

if [ ! -s "$ORIGINAL" ]; then
  echo -n "Original file missing "
  exit 1
fi

if [ ! -s "$THUMBNAIL" ]; then
  echo -n "Thumbnail file missing or empty "
  exit 1
fi

compare_with_findimagedupes_EqualFingerPrint() {
  local original="$1"
  local thumbnail="$2"
  OUTPUT="$(findimagedupes -q --threshold=80% --verbosity=fingerprint "$original" "$thumbnail" | head -n2 | cut -d ' ' -f 1)"
#   echo -e "OUTPUT: $OUTPUT"
  FINGERPRINT_ORIGINAL=$(head -n1 <<< "$OUTPUT")
  FINGERPRINT_THUMBNAIL=$(tail -n1 <<< "$OUTPUT")

#   echo
#   echo "FINGERPRINT_ORIGINAL: $FINGERPRINT_ORIGINAL"
#   echo
#   echo "FINGERPRINT_THUMBNAIL: $FINGERPRINT_THUMBNAIL"
#   echo

  if [ "a$FINGERPRINT_ORIGINAL" == "a$FINGERPRINT_THUMBNAIL" ]; then
    exit 0
  else
    echo -n "Fingerprint differ [$ORIGINAL] [$THUMBNAIL]"
    exit 2
  fi
}

compare_with_findimagedupes() {
  local original="$1"
  local thumbnail="$2"
  OUTPUT="$(findimagedupes -q --threshold=85% --verbosity=fingerprint "$original" "$thumbnail")"
#   echo -e "OUTPUT:\n$OUTPUT"
  LINE_COUNT=$(wc -l <<< "$OUTPUT")
  # Remove leading space (this is required on macOS)
  LINE_COUNT="${LINE_COUNT#"${LINE_COUNT%%[![:space:]]*}"}"
#   echo -e "LINE_COUNT: $LINE_COUNT"
#   FINGERPRINT_ORIGINAL=$(head -n2 <<< "$OUTPUT" | head -n1 | cut -d ' ' -f 1)
#   FINGERPRINT_THUMBNAIL=$(head -n2 <<< "$OUTPUT" | tail -n1 | cut -d ' ' -f 1)

#   echo
#   echo "FINGERPRINT_ORIGINAL: $FINGERPRINT_ORIGINAL"
#   echo
#   echo "FINGERPRINT_THUMBNAIL: $FINGERPRINT_THUMBNAIL"
#   echo

  if [ "a$LINE_COUNT" == "a3" ]; then
    exit 0
  else
    #echo -n "Pictures are not similar [$ORIGINAL] [$THUMBNAIL]"
    exit 2
  fi
}

rescale_with_ffmpeg_lanczsos () {
  # TODO
  ffmpeg -i Canon_DIGITAL_IXUS_400.jpg -vf scale=100:75 -sws_flags lanczos Canon_DIGITAL_IXUS_400_ffmpeg.jpg
}

if [ "$engine" == "findimagedupes" ]; then
    compare_with_findimagedupes "$ORIGINAL" "$THUMBNAIL"
elif [ "$engine" == "findimagedupes_with_prior_resize" ]; then
  if [ ! -f "${THUMBNAIL}_ref" ]; then
    SIZE=$(identify -quiet -ping -format '%wx%h' "$THUMBNAIL" )
    # We use -auto-orient so that the _ref is rotated and exif Orientation tag is reset.
    # Otherwise the comparison with findimagedupes would fail because the raw image wouldn't have been rotated.
    # https://www.imagemagick.org/script/command-line-options.php#auto-orient
    convert -quiet -auto-orient -resize "$SIZE" "$ORIGINAL" "${THUMBNAIL}_ref"
  fi
  compare_with_findimagedupes "${THUMBNAIL}_ref" "$THUMBNAIL"
fi
