#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

VARIANTS="standard"
TEST_OUTPUT_DIR="output/test_output"
TEST_PICS="output/test_pics"

PICTURE_WITH_THUMBNAIL="Canon_40D.jpg"

# The Ricoh_Caplio_RR330.jpg has a tiff thumbnail (as per exiv2) but is detected by android as not having one.
PICTURE_WITHOUT_THUMBNAIL_AS_PER_ANDROID="Ricoh_Caplio_RR330.jpg"

# The Fujifilm_FinePix_E500.jpg has no thumbnail as per exiv2 & exiftool
PICTURE_WITHOUT_THUMBNAIL="Fujifilm_FinePix_E500.jpg"

PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG=exif-org/kodak-dc210.jpg
PICTURE_WITHOUT_THUMBNAIL_WITH_EXIF_BUT_MISSING_MANDATORY_EXIF_TAG=orientation/portrait_4.jpg

PICTURE_WITHOUT_EXIF=noexif.jpg

PICTURE_EXIV2_ERROR=Nikon_COOLPIX_P1.jpg
PICTURE_EXIV2_WARNING=Konica_Minolta_DiMAGE_Z3.jpg

PICTURE_LIBEXIF_ERROR=Kodak_CX7530.jpg

# To test that Pixymeta skips the file
PICTURE_WITH_MAKERNOTES="Canon_DIGITAL_IXUS_400.jpg"

# Pictures having various values for the ORIENTATION tag.
#1 = Horizontal (normal)
#2 = Mirror horizontal
#3 = Rotate 180
#4 = Mirror vertical
#5 = Mirror horizontal and rotate 270 CW
#6 = Rotate 90 CW
#7 = Mirror horizontal and rotate 90 CW
#8 = Rotate 270 CW
PICTURE_ORIENTATION_1=orientation/landscape_1.jpg
PICTURE_ORIENTATION_2=orientation/portrait_2.jpg
PICTURE_ORIENTATION_3=orientation/landscape_3.jpg
PICTURE_ORIENTATION_4=orientation/landscape_4.jpg
PICTURE_ORIENTATION_5=orientation/landscape_5.jpg
PICTURE_ORIENTATION_6=orientation/portrait_6.jpg
PICTURE_ORIENTATION_7=orientation/portrait_7.jpg
PICTURE_ORIENTATION_8=orientation/portrait_8.jpg

# Errors in AAE only
PICTURE_AAE_ERROR=tests/87_OSError.jpg

# Errors in Exiv2 & pixymeta
PICTURE_PIXYMETA_ERROR=tests/45-gps_ifd.jpg

EMPTY_FILE=empty_file
TEXT_FILE=text_file.txt
TEXT_WITH_JPG_EXT=text_with_jpg_extension.jpg

GREEN='\033[0;32m'
RED='\033[0;31m'
WHITE='\033[0;37m'
RESET='\033[0m'
YELLOW='\033[0;33m'

NO_IS_SAME_PICTURE_CHECK=0

if [ -n "$1" ]; then
    APIs="$1"
else
    APIs="$(ls $TEST_OUTPUT_DIR)"
fi

if [ ! -d $TEST_OUTPUT_DIR ]; then
  echo "$TEST_OUTPUT_DIR dir is missing. Exiting."
  exit 1
fi

if [ ! -d $TEST_PICS ]; then
  echo "$TEST_PICS dir is missing. Exiting."
  exit 1
fi

if [ $(uname) = "Darwin" ]; then
  # Use gnu-sed on macos (provided by homebrew or macports)
  SED=gsed
else
  SED=sed
fi

check_apps() {
  # https://stackoverflow.com/questions/592620/check-if-a-program-exists-from-a-bash-script
  local MISSING_APPS
  local RESULT

  while [[ $# -gt 0 ]]; do
    key="$1"
    RESULT="$RESULT $key "

    if command -v "$key" >/dev/null 2>&1; then
      RESULT="$RESULT OK\n"
    else
      RESULT="$RESULT missing\n"

      if [ "$key" = "findimagedupes" ]; then
        MISSING_APPS_OPTIONAL="$MISSING_APPS_OPTIONAL $key"
        NO_IS_SAME_PICTURE_CHECK=1
      else
        MISSING_APPS_MANDATORY="$MISSING_APPS_MANDATORY $key"
      fi
    fi
    shift # past argument or value
  done
  #echo -e "$RESULT" | column -t

  if [ ! -z "$MISSING_APPS_OPTIONAL" ]; then
    echo "Missing apps (optional) : $MISSING_APPS_OPTIONAL"
  fi
  if [ ! -z "$MISSING_APPS_MANDATORY" ]; then
    echo "Missing apps (mandatory) : $MISSING_APPS_MANDATORY"
    echo "Stopping"
    exit 1
  fi
}

success() {
  echo -e "${GREEN}PASS${RESET}"
}

skipped() {
  echo -e -n "${YELLOW}SKIP${RESET}"
  if [ ! a"$1" = "a" ]; then
    echo ": $1"
  else
    echo
  fi
}

failure() {
  echo -e -n "${RED}FAIL${RESET}"
  if [ ! a"$1" = "a" ]; then
    echo ": $1"
  else
    echo
  fi
}

# When there is an error or an exception in the log.txt, there is a line break in the log of that line.
# To ease some tests, remove it.
# \xC2\xA0 is for the non-breaking space
update_log_to_one_liner() {
  local logfile="$1"
  $SED ':a;N;$!ba;s/\n\xC2\xA0/\xC2\xA0/g' "$logfile"  > "${logfile}_tmp"
  iconv --to utf8 "${logfile}_tmp" > "${logfile}1"
  rm "${logfile}_tmp"
}

remove_all_metadata() {
  local picture="$1"
  if [ ! -f "${picture}_noMetaData" ]; then
    cp "${picture}" "${picture}_noMetaData"
    exiftool -q -q -all:all= -overwrite_original "${picture}_noMetaData" > /dev/null 2>&1
  fi
}

extract_thumbnails() {
  local picture="$1"
  if [ ! -f "${picture}_tb" ]; then
    exiftool -q -q -b -ThumbnailImage "${picture}" > "${picture}_tb"
  fi
}

if which exiv2 > /dev/null; then
  CHECK_NO_EXIF=$(LANG=C exiv2 "$TEST_PICS/$PICTURE_WITHOUT_EXIF" 2>&1 | grep 'No Exif data found in the file' )
  if [[ ! "$CHECK_NO_EXIF" =~ "No Exif data found in the file" ]] ; then
    echo "$TEST_PICS/$PICTURE_WITHOUT_EXIF contains exif data while it shouldn't. Choose another file".
    # TODO when file is tested
    #exit
  fi
fi

check_apps findimagedupes exiv2 exiftool convert $SED

for API in $APIs; do
  for VARIANT in $VARIANTS; do

    test_number=0

    echo -e "API: ${API} - VARIANT: $VARIANT"

    # Check dirs: ThumbAdder exists?
    test_number=$(( test_number+1 ))
    echo -en "[$test_number] ThumbAdder exists: \t"
    TESTNAME=addThumbsSettingsDefault
    if [ -d "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder" ]; then
        success
    else
        failure
    fi

    # Alternative WorkingDir exists?
    test_number=$(( test_number+1 ))
    echo -en "[$test_number] Alternative WorkingDir name (JustSomething) exists: \t"
    TESTNAME=addThumbsAltWorkDir
    if [ -d "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/JustSomething" ]; then
        success
    else
        failure
    fi

    #Skip having thumbnail ON
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsUpdateInSourceOffWithoutDestOverwrite"
    echo -en "[$test_number] Skip having thumbnail ON + pic without thumbnail. ($API, $VARIANT): \t"
    if [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ]; then
        success
    else
        failure
    fi

    test_number=$(( test_number+1 ))
    echo -en "[$test_number] Skip having thumbnail ON + pic with thumbnail. ($API, $VARIANT): \t"
    if [ ! -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" ] \
      && [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" ]; then
        success
    else
        failure
    fi

    #Skip having thumbnail OFF
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsSkipOff"
    echo -en "[$test_number] Skip having thumbnail OFF + pic without thumbnail. ($API, $VARIANT): \t"
    if [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && ! diff "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" > /dev/null; then
        success
    else
        failure
    fi

    test_number=$(( test_number+1 ))
    echo -en "[$test_number] Skip having thumbnail OFF + pic with thumbnail. ($API, $VARIANT): \t"
    if [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" ] \
      && [ -e "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" ] \
      && ! diff "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}" > /dev/null; then
        success
    else
        failure
    fi

    for external_storage in on off; do
      # Check timestamps are kept in .bak
      if [ $external_storage = "off" ]; then
        TESTNAME="addThumbsSettingsDefault"
        if [ "$API" -ge 30 ]; then LBL="${external_storage}" ; else LBL="N/A-${external_storage}" ; fi
      else
        TESTNAME="addThumbsSettingsAllFilesAccessOn"
        if [ "$API" -ge 30 ]; then LBL="${external_storage}" ; else LBL="N/A-${external_storage}" ; fi
      fi

      test_number=$(( test_number+1 ))
      echo -en "[$test_number] Timestamps (in .bak) ExternalStorage=${LBL}. ($API, $VARIANT): \t"
      if [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -nt "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ] \
        && [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -ot "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ]; then
        success
      else
        if [ "$API" -lt 30 ] && [ $external_storage = "on" ]; then
          skipped "API < 30, test not relevant"
        else
          failure
        fi
      fi

      # Check timestamps are kept when overwriting file in source
      test_number=$(( test_number+1 ))
      echo -en "[$test_number] Timestamps (overwrite in source) ExternalStorage=${LBL}. ($API, $VARIANT): \t"
      if [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -nt "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ] \
        && [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -ot "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ]; then
        success
      else
        if [ "$API" -lt 30 ] && [ $external_storage = "on" ]; then
          skipped "API < 30, test not relevant"
        elif [ "$API" -ge 30 ] && [ $external_storage = "off" ]; then
          failure "(expected)"
        else
          failure
        fi
      fi
    done

    for external_storage in on off; do
      # Check timestamps are kept in .new
      if [ $external_storage = "off" ]; then
        TESTNAME="addThumbsSettingsUpdateInSourceOffWithoutDestOverwrite"
        if [ "$API" -ge 30 ]; then LBL="${external_storage}" ; else LBL="N/A-${external_storage}" ; fi
      else
        TESTNAME="addThumbsSettingsAllFilesAccessOnUpdateInSourceOff"
        if [ "$API" -ge 30 ]; then LBL="${external_storage}" ; else LBL="N/A-${external_storage}" ; fi
      fi

      test_number=$(( test_number+1 ))
      echo -en "[$test_number] Timestamps (in .new) ExternalStorage=${LBL}. ($API, $VARIANT): \t"
      if [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -nt "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ] \
        && [ ! "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" \
        -ot "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" ]; then
        success
      else
        if [ $external_storage = "off" ] && [ "$API" -ge 30 ]; then
          failure "(expected)"
        else
          if [ "$API" -lt 30 ] && [ $external_storage = "on" ]; then
            skipped "API < 30, test not relevant"
          else
            failure
          fi
        fi
      fi
    done

    # Don't create backup
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsCreateBackupOff"
    echo -en "[$test_number] Don't create backup. ($API, $VARIANT): \t"
    if [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ]; then
        success
    else
        failure
    fi

    # Backup WITHOUT preexisting backup file
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsDefault"
    echo -en "[$test_number] Create backup without preexisting backup file. ($API, $VARIANT): \t"
    if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] &&
      diff -q "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" >/dev/null 2>&1; then
        success
    else
        failure
    fi

    # Check that backup with preexisting backup file is not overwritten
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsDefaultTwoRuns"
    echo -en "[$test_number] Check that backup with preexisting backup file is not overriden. ($API, $VARIANT): \t"
    if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] &&
      diff -q "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" >/dev/null 2>&1 &&
      ! diff -q "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" >/dev/null 2>&1; then
        success
    else
        failure
    fi

    # Update in source
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsDefault"
    echo -en "[$test_number] Update in source. ($API, $VARIANT): \t"
    if [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && ! diff "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" > /dev/null ; then
        success
    else
        failure
    fi

    # Don't update in source - with overwrite in destination
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsUpdateInSourceOffWithDestOverwrite"
    echo -en "[$test_number] Don't update in source with dest overwrite + pic without thumbnail. ($API, $VARIANT): \t"
    if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" ] \
      && diff "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" > /dev/null \
      && ! diff "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}" "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" > /dev/null; then
        success
    else
        failure
    fi

    # Don't update in source - without overwrite in destination
    # Check that in the logs we have this text: "Overwriting picture in destination dir not allowed (see settings)"
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsUpdateInSourceOffWithoutDestOverwrite"
    echo -en "[$test_number] Don't update in source without dest overwrite (2runs) + pic without thumbnail. ($API, $VARIANT): \t"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" ] && \
      grep "test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}.*Overwriting picture in destination dir not allowed (see settings)" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test Exif libraries
    for TESTNAME in addThumbsSettingsExiv2 \
      addThumbsSettingsLibExif \
      addThumbsSettingsAEE \
      addThumbsSettingsPixymeta; do

      test_number=$(( test_number+1 ))
      echo -en "[$test_number] $TESTNAME, pic without Tn. ($API, $VARIANT): \t"
      PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL}"
      if [ -f "$PIC" ] && \
        ! diff "$PIC" "$TEST_PICS/${PICTURE_WITHOUT_THUMBNAIL}" > /dev/null; then
        # Extract thumbnail
        extract_thumbnails "$PIC"
        if [ -s "$PIC"_tb ]; then
          success
        else
          failure "no thumbnail in output"
        fi
        rm -f "$PIC"_tb
      else
        success
      fi

      test_number=$(( test_number+1 ))
      echo -en "[$test_number] $TESTNAME, pic with Tn. ($API, $VARIANT): \t"
      PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL}"
      if [ -f "$PIC" ] && \
        ! diff "$PIC" "$TEST_PICS/${PICTURE_WITH_THUMBNAIL}" > /dev/null; then
        # Extract thumbnail
        extract_thumbnails "$TEST_PICS/${PICTURE_WITH_THUMBNAIL}"
        extract_thumbnails "$PIC"
        if [ -f "${PIC}_tb" ] &&
          ! diff "${PIC}_tb" "$TEST_PICS/${PICTURE_WITH_THUMBNAIL}"_tb > /dev/null; then
          success
        else
          failure "thumbnails are not different"
        fi
        rm -f "$TEST_PICS/${PICTURE_WITH_THUMBNAIL}"_tb
      else
        if [ $TESTNAME = addThumbsSettingsAEE ]; then
          failure "expected: AEE shouldn't update thumbnail even if we try to unset 'skipHavingThumbnail')"
        else
          failure
        fi
      fi
    done

    # Check that .nomedia file was created in ThumbAdder/DCIM.{bak,new}/.nomedia
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsUpdateInSourceOffWithoutDestOverwrite"
    echo -en "[$test_number] Check that .nomedia files were created in ThumbAdder/DCIM.{bak,new}/.nomedia. ($API, $VARIANT): \t"
    if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/.nomedia" ] && \
      [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/.nomedia" ] ; then
        success
    else
        failure
    fi

    # Test Exiv2 skip from log level warn option
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsExiv2"
    echo -en "[$test_number] Exiv2 skip from log level warn option. ($API, $VARIANT): \t"
     update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_EXIV2_WARNING}... Skipping (ERROR): exiv2: Warning" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test Exiv2 skip from log level error option
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsExiv2SkipOnLogLevelError"
    echo -en "[$test_number] Exiv2 skip from log level error option. ($API, $VARIANT): \t"
     update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_EXIV2_WARNING}.*Done$" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test Exiv2 skip from log level none option
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsExiv2SkipOnLogLevelNone"
    echo -en "[$test_number] Exiv2 skip from log level none option. ($API, $VARIANT): \t"
     update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_EXIV2_ERROR}.*Done$" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test Libexif skip on error TRUE
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsLibExif"
    echo -en "[$test_number] Libexif skip on error TRUE ($API, $VARIANT): \t"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_LIBEXIF_ERROR}... Skipping (ERROR): libexif error" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test Libexif skip on error FALSE
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsLibExifSkipOnErrorOFF"
    echo -en "[$test_number] Libexif skip on error FALSE. ($API, $VARIANT): \t"
     update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_LIBEXIF_ERROR}.*Done$" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # Test pixymeta skip containing makernote
    test_number=$(( test_number+1 ))
    TESTNAME="addThumbsSettingsPixymeta"
    echo -en "[$test_number] Pixymeta skip containing makernote ($API, $VARIANT): \t"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_WITH_MAKERNOTES}... MakerNotes present in metadata" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
        success
    else
        failure
    fi

    # MandatoryExifTags: FixON, withoutThumb, withPartialEXIF. Check has at least 1 tag
    TESTNAME=addThumbsSettingsDefault
    test_number=$(( test_number+1 ))
    echo -ne "[$test_number] MandatoryExifTags: FixON, withoutThumb, withPartialEXIF ($TESTNAME). ($API, $VARIANT): \t"
    PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_THUMBNAIL_WITH_EXIF_BUT_MISSING_MANDATORY_EXIF_TAG}"
    if ./has_mandatory_tags.sh "$PIC" "one"; then
      success
    else
      failure
    fi

    # MandatoryExifTags: FixON, withoutThumb, withoutEXIF. Check has all 6 mandatory tags
    TESTNAME=addThumbsSettingsDefault
    test_number=$(( test_number+1 ))
    echo -ne "[$test_number] MandatoryExifTags: FixON, withoutThumb, withoutEXIF ($TESTNAME). ($API, $VARIANT): \t"
    PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITHOUT_EXIF}"
    if ./has_mandatory_tags.sh "$PIC" "all"; then
      success
    else
      failure
    fi

    # MandatoryExifTags: FixON, withThumb, withPartialEXIF. Check has at least 1 tag
    TESTNAME=addThumbsSettingsExiv2
    test_number=$(( test_number+1 ))
    echo -ne "[$test_number] MandatoryExifTags: FixON, withThumb, withPartialEXIF. ($TESTNAME). ($API, $VARIANT): \t"
    PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}....*Done" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null &&
      ./has_mandatory_tags.sh "$PIC" "one"; then
      success
    else
      failure
    fi

    # MandatoryExifTags: FixON, SkipHasThumbnailsON, withThumb, withPartialEXIF. Check file is processed.
    TESTNAME=addThumbsSettingsDefault
    test_number=$(( test_number+1 ))
    echo -ne "[$test_number] MandatoryExifTags: FixON, SkipHasThumbnailsON, withThumb, withPartialEXIF. ($TESTNAME). ($API, $VARIANT): \t"
    PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}.*Thumbnail present but some fields are missing..*Updating thumbnail" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
      success
    else
      failure
    fi

    # MandatoryExifTags: FixMissingOFF, SkipHasThumbnailsON, withThumb, withPartialEXIF. Check file is not processed
    TESTNAME=addThumbsSettingsFixMissingOff
    test_number=$(( test_number+1 ))
    echo -ne "[$test_number] MandatoryExifTags: FixMissingOFF, SkipHasThumbnailsON, withThumb, withPartialEXIF. ($TESTNAME). ($API, $VARIANT): \t"
    PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}"
    update_log_to_one_liner "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt"
    if grep "${PICTURE_WITH_THUMBNAIL_BUT_MISSING_MANDATORY_EXIF_TAG}.*Skipping (has thumbnail)" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/log.txt1" > /dev/null ; then
      success
    else
      failure
    fi

    # Test if output has mandatory exif tags
    for TESTNAME in \
        addThumbsSettingsExiv2 \
        addThumbsSettingsLibExif \
        addThumbsSettingsAEE \
        addThumbsSettingsPixymeta; do
      test_number=$(( test_number+1 ))
      EXIFTAG_PROBLEMS=""
      echo -ne "[$test_number] Check mandatory exif tags ($TESTNAME). ($API, $VARIANT): \t"
      #for file in $(cd $TEST_PICS && find -iname "*.jpg" -or -iname "*.jpeg" | sort); do
      # Only check files that were actually processed (otherwise it would check files that ETA didn't process)
      for file in $(cd "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}" && find . -iname "*.jpg" -or -iname "*.jpeg" | sort); do
        PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/$file"
        if ./has_mandatory_tags.sh "$PIC" "one"; then
          echo -en "${GREEN}.${RESET}"
        else
          echo -en "${RED}.${RESET}"
          EXIFTAG_PROBLEMS="${PIC}_exif $EXIFTAG_PROBLEMS"
        fi
      done
      if [ "a$EXIFTAG_PROBLEMS" == "a" ]; then
          success
      else
          failure "See list of files with missing exif tags"
          tr ' ' '\n' <<< "$EXIFTAG_PROBLEMS"
      fi
    done


    # Test Thumbnail Rotation
    test_number=$(( test_number+1 ))
    TESTNAME=addThumbsSettingsRotateOff
    THUMBNAIL_PROBLEMS=
    echo -en "[$test_number] Disable Thumbnail rotation (with SAF). ($API, $VARIANT): \t"
    if [ $NO_IS_SAME_PICTURE_CHECK -eq 1 ]; then
      skipped "findimagedupes tool missing"
    else
    for picture in \
      $PICTURE_ORIENTATION_1 \
      $PICTURE_ORIENTATION_2 \
      $PICTURE_ORIENTATION_3 \
      $PICTURE_ORIENTATION_4 \
      $PICTURE_ORIENTATION_5 \
      $PICTURE_ORIENTATION_6 \
      $PICTURE_ORIENTATION_7 \
      $PICTURE_ORIENTATION_8 \
      ; do
      PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${picture}"
      remove_all_metadata "${PIC}"
      extract_thumbnails "$PIC"
      if ./is_same_picture.sh "$PIC"_noMetaData "$PIC"_tb; then
        #echo $PIC
        echo -en "${GREEN}.${RESET}"
      else
        echo -en "${RED}.${RESET}"
        THUMBNAIL_PROBLEMS="${PIC}_tb $THUMBNAIL_PROBLEMS"
      fi
    done
    if [ "a$THUMBNAIL_PROBLEMS" == "a" ]; then
      success
    else
      failure "See list of dissimilar files below"
      tr ' ' '\n' <<< "$THUMBNAIL_PROBLEMS"
    fi
    fi

    test_number=$(( test_number+1 ))
    THUMBNAIL_PROBLEMS=
    echo -en "[$test_number] Test Thumbnail rotation with 'Files' instead of 'SAF'. ($API, $VARIANT): \t"
    # Select the testname (because running without SAF can only work if AllFilesAccess is granted)
    TESTNAME=
    #TESTNAME=addThumbsSettingsExiv2withoutSAF
    if [ "$API" -ge 30 ] && [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_addThumbsSettingsExiv2withoutSAF_AllFilesAccess/log.txt" ]; then
      TESTNAME=addThumbsSettingsExiv2withoutSAF_AllFilesAccess
    elif [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_addThumbsSettingsExiv2withoutSAF/log.txt" ]; then
      TESTNAME=addThumbsSettingsExiv2withoutSAF
    else
      failure "Couldn't find test case"
    fi
    if [ $NO_IS_SAME_PICTURE_CHECK -eq 1 ]; then
      skipped "findimagedupes tool missing"
    else
    if [ -n "$TESTNAME" ]; then
      echo -n " [Testing with $TESTNAME] "
      for picture in \
        $PICTURE_ORIENTATION_1 \
        $PICTURE_ORIENTATION_2 \
        $PICTURE_ORIENTATION_3 \
        $PICTURE_ORIENTATION_4 \
        $PICTURE_ORIENTATION_5 \
        $PICTURE_ORIENTATION_6 \
        $PICTURE_ORIENTATION_7 \
        $PICTURE_ORIENTATION_8 \
        ; do
        ORIG_PIC="$TEST_PICS/${picture}"
        PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/${picture}"
        extract_thumbnails "$PIC"
        if ./is_same_picture.sh "$ORIG_PIC" "$PIC"_tb; then
          #echo $PIC
          echo -en "${GREEN}.${RESET}"
        else
          echo -en "${RED}.${RESET}"
          THUMBNAIL_PROBLEMS="${PIC}_tb $THUMBNAIL_PROBLEMS"
        fi
      done
      if [ "a$THUMBNAIL_PROBLEMS" == "a" ]; then
        success
      else
        failure "See list of dissimilar files below"
        tr ' ' '\n' <<< "$THUMBNAIL_PROBLEMS"
      fi
    fi
    fi

    # Test thumbnail content (check that it is similar to orignal)
    for TESTNAME in \
        addThumbsSettingsExiv2 \
        addThumbsSettingsLibExif \
        addThumbsSettingsAEE \
        addThumbsSettingsPixymeta; do
      test_number=$(( test_number+1 ))
      THUMBNAIL_PROBLEMS=""
      echo -ne "[$test_number] Check thumbnail ($TESTNAME). ($API, $VARIANT): \t"
      if [ $NO_IS_SAME_PICTURE_CHECK -eq 1 ]; then
        skipped "findimagedupes tool missing"
      else
      #for file in $(cd $TEST_PICS && find -iname "*.jpg" -or -iname "*.jpeg" | sort); do
      # Only check files that were actually processed (otherwise it would check files that ETA didn't process)
      for file in $(cd "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}" && find . -iname "*.jpg" -or -iname "*.jpeg" | sort); do
        ORIG_PIC="$TEST_PICS/$file"
        PIC="$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/DCIM/test_${VARIANT}_${TESTNAME}/$file"
        extract_thumbnails "$PIC"
        if ./is_same_picture.sh "$ORIG_PIC" "$PIC"_tb; then
        #echo $PIC
          echo -en "${GREEN}.${RESET}"
        else
          echo -en "${RED}.${RESET}"
          THUMBNAIL_PROBLEMS="${PIC}_tb $THUMBNAIL_PROBLEMS"
        fi
      done
      if [ "a$THUMBNAIL_PROBLEMS" == "a" ]; then
        success
      else
        failure "See list of dissimilar files below"
        tr ' ' '\n' <<< "$THUMBNAIL_PROBLEMS"
      fi
      fi
    done

    test_number=$(( test_number+1 ))
    PROBLEMS=""
    TESTNAME=syncListTest
    echo -ne "[$test_number] Check Sync 'list' - input=processed & deleted ($TESTNAME). ($API, $VARIANT): \t"
    for picture in \
        Reconyx_HC500_Hyperfire.jpg \
        mobile/jolla.jpg \
        orientation/portrait_1.jpg \
        orientation/portrait_2.jpg \
        orientation/portrait_3.jpg \
        orientation/portrait_4.jpg \
        orientation/portrait_5.jpg \
        orientation/portrait_6.jpg \
        orientation/portrait_7.jpg \
        orientation/portrait_8.jpg \
        tests/87_OSError.jpg \
        ; do
      if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" ] &&
        grep "ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        grep "ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" ] &&
        [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" ]
        then
        echo -en "${GREEN}.${RESET}"
      else
        echo -en "${RED}.${RESET}"
        PROBLEMS="${picture} $PROBLEMS"
      fi
    done
    if [ "a$PROBLEMS" == "a" ]; then
      success
    else
      failure "See list of problematic files below"
      tr ' ' '\n' <<< "$PROBLEMS"
    fi

    test_number=$(( test_number+1 ))
    PROBLEMS=""
    TESTNAME=syncListTest
    echo -ne "[$test_number] Check Sync 'list' - input=NOT processed & deleted ($TESTNAME). ($API, $VARIANT): \t"
    # This picture was deleted but it was not processed. We expect it is not in the sync_log.txt
    for picture in \
        tests/42_IndexError.jpg \
        ; do
      if  [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" ] &&
        ! grep "ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        ! grep "ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" ] &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" ]
        then
        echo -en "${GREEN}.${RESET}"
      else
        echo -en "${RED}.${RESET}"
        PROBLEMS="${picture} $PROBLEMS"
      fi
    done
    if [ "a$PROBLEMS" == "a" ]; then
      success
    else
      failure "See list of problematic files below"
      tr ' ' '\n' <<< "$PROBLEMS"
    fi

    test_number=$(( test_number+1 ))
    PROBLEMS=""
    TESTNAME=syncDeleteTest
    echo -ne "[$test_number] Check Sync 'delete' - input=processed & deleted ($TESTNAME). ($API, $VARIANT): \t"
    for picture in \
        Reconyx_HC500_Hyperfire.jpg \
        mobile/jolla.jpg \
        orientation/portrait_1.jpg \
        orientation/portrait_2.jpg \
        orientation/portrait_3.jpg \
        orientation/portrait_4.jpg \
        orientation/portrait_5.jpg \
        orientation/portrait_6.jpg \
        orientation/portrait_7.jpg \
        orientation/portrait_8.jpg \
        tests/87_OSError.jpg \
        ; do
      if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" ] &&
        grep "ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}.*Done" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        grep "ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}.*Done" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" ] &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" ]
        then
        echo -en "${GREEN}.${RESET}"
      else
        echo -en "${RED}.${RESET}"
        PROBLEMS="${picture} $PROBLEMS"
      fi
    done
    if [ "a$PROBLEMS" == "a" ]; then
      success
    else
      failure "See list of problematic files below"
      tr ' ' '\n' <<< "$PROBLEMS"
    fi

    test_number=$(( test_number+1 ))
    PROBLEMS=""
    TESTNAME=syncDeleteTest
    echo -ne "[$test_number] Check Sync 'delete' - input=NOT processed & deleted ($TESTNAME). ($API, $VARIANT): \t"
    # This picture was deleted but it was not processed. We expect it is not in the sync_log.txt
    for picture in \
        tests/42_IndexError.jpg \
        ; do
      if [ -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" ] &&
        ! grep "ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}.*Done" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        ! grep "ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}.*Done" "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/sync_log.txt" > /dev/null &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.bak/test_${VARIANT}_${TESTNAME}/${picture}" ] &&
        [ ! -f "$TEST_OUTPUT_DIR/$API/${VARIANT}_${TESTNAME}/ThumbAdder/DCIM.new/test_${VARIANT}_${TESTNAME}/${picture}" ]
        then
        echo -en "${GREEN}.${RESET}"
      else
        echo -en "${RED}.${RESET}"
        PROBLEMS="${picture} $PROBLEMS"
      fi
    done
    if [ "a$PROBLEMS" == "a" ]; then
      success
    else
      failure "See list of problematic files below"
      tr ' ' '\n' <<< "$PROBLEMS"
    fi

    echo
  done
done
