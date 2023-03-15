#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

set -e

# Mandator tags as per https://exiftool.org/TagNames/EXIF.html
# 
# 0x0002 	InteropVersion 	undef!: 	InteropIFD 	 
# 
# 0x0103 	Compression 	int16u!: 	IFD0 	--> EXIF Compression Values
# 0x011a 	XResolution 	rational64u: 	IFD0 	 
# 0x011b 	YResolution 	rational64u: 	IFD0 	 
# 0x0128 	ResolutionUnit 	int16u: 	IFD0 	(the value 1 is not standard EXIF)
# 0x0213 	YCbCrPositioning 	int16u!: 	IFD0 	1 = Centered
# 
# 0x9000 	ExifVersion 	undef: 	ExifIFD 	 
# 0x9101 	ComponentsConfiguration 	undef[4]!: 	ExifIFD 	
# 0xa000 	FlashpixVersion 	undef: 	ExifIFD 	 
# 0xa001 	ColorSpace 	int16u: 	ExifIFD 	(the value of 0x2 is not standard EXIF. Instead, an Adobe RGB 
# 0xa002 	ExifImageWidth 	int16u: 	ExifIFD 	(called PixelXDimension by the EXIF spec.)
# 0xa003 	ExifImageHeight 	int16u: 	ExifIFD 	(called PixelYDimension by the EXIF spec.)
# 

# In exiv2 it would become:
# Exif.Photo.ExifVersion
# Exif.Photo.ComponentsConfiguration
# Exif.Photo.FlashpixVersion
# Exif.Photo.ColorSpace
# Exif.Photo.PixelXDimension
# Exif.Photo.PixelYDimension

# This script checks if at least one of the mandatory tags of ExifIFD group are present. This is sufficient
# to have the ExifIFD group present. The ExifIFD group permits the thumbnail to be seen.
# - Libexif fixes correctly by adding all 6, except Exif.Photo.ComponentsConfiguration
# - In Exiv2 we made the choice to add all 6 tags only if there is no Exif data at all, or if there isn't at least one of the mandatory tags.

# Call this script with MODE=all (to check all tags are present) or MODE=one (to require only at least one tag)

DEBUG=0
PICTURE="$1"
MODE="$2"

if [ $DEBUG -eq 1 ]; then
  set -x
fi

TAG_COUNT=$(grep . < mandatory_exif_tags.txt | wc -l )

if [ ! -f "${PICTURE}_exif" ]; then
  exiv2 -p a "$PICTURE" > "${PICTURE}_exif" 2>/dev/null
fi

if [ $DEBUG -eq 1 ]; then
  grep -f mandatory_exif_tags.txt "${PICTURE}_exif"
fi
MATCHED_LINES_COUNT=$(grep -f mandatory_exif_tags.txt "${PICTURE}_exif" -c || true)

if [ "$MODE" = "all" ] || [ "a$MODE" = "a" ]; then
  if [ "$TAG_COUNT" -eq "$MATCHED_LINES_COUNT" ]; then
    exit 0
  else
    exit 1
  fi
elif [ "$MODE" = "one" ]; then
  # We require at least one tag of the 6 mandatory tags.
  if [ "$MATCHED_LINES_COUNT" -ge 1 ]; then
    exit 0
  else
    exit 1
  fi
fi

if [ $DEBUG -eq 1 ]; then
  set +x
fi
