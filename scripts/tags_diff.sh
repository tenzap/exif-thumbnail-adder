#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/tags_diff.sh
#   ./scripts/tags_diff.sh difftool
#   ./scripts/tags_diff.sh difftool old_version new_version
#   ./scripts/tags_diff.sh difftool old_branch new_branch
#
# Exit code:
#   - 1: there was an error
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if [ "$PWD/scripts" = "$SCRIPT_DIR" ]; then
    echo "Script is called from $PWD: OK!"
else
    echo
    echo "Must be run from project root dir: ./scripts/$( basename "${BASH_SOURCE[0]}" )"
    echo
    exit 1;
fi

ETA_REPO_PATH="$PWD"
PICS_REPO_PATH="$(realpath "../pic-exif-samples/")"
DIFFTOOLS="exiftool exiv2 identitfy-verbose identitfy-format"

run_git_diff() {
  mkdir -p "$ETA_REPO_PATH/tests/output/tags_diff/$3"
  echo "Running diff: $1 vs $2"
  git diff "$1" "$2"  > "$ETA_REPO_PATH/tests/output/tags_diff/$3/$1=$2.$DIFFTOOL.diff" 2>&1
}

if [ -n "$1" ]; then
  if [ -n "$2" ] && [ -z "$3" ]; then
    echo "Usage"
    echo "./scripts/$( basename "${BASH_SOURCE[0]}") difftool old_branch new_branch"
    echo "./scripts/$( basename "${BASH_SOURCE[0]}") difftool old_version new_version"
    echo
    exit 1
  fi
fi

cd "$PICS_REPO_PATH"

git checkout master > /dev/null

if [ -z "$1" ]; then
  echo "Enter diff tool name. [all]"
  echo "  all"
  for tool in $DIFFTOOLS; do
    echo "  $tool"
  done
  echo
  read -r

  if [ -z "$REPLY" ]; then
    DIFFTOOL_REQ="all"
  else
    DIFFTOOL_REQ="${REPLY}"
  fi

else
  DIFFTOOL_REQ="$1"

  if git show-ref --quiet refs/heads/"$2"; then
    old_branch="$2"
  else
    old_version="$2"
  fi
  if git show-ref --quiet refs/heads/"$3"; then
    new_branch="$3"
  else
    new_version="$3"
  fi

  if [ -n "$old_branch" ] && [ -n "$new_branch" ]; then
    is_ok=1
  fi

  if [ -n "$old_version" ] && [ -n "$new_version" ]; then
    is_ok=1
  fi

  if [ ! a"$is_ok" = a"1" ]; then
    echo "Invalid branch or version."
    exit 1
  fi

fi

if [ a"$DIFFTOOL_REQ" = a"all" ]; then
  DIFFTOOL_REQ="$DIFFTOOLS"
fi

LATEST_VER="$(git branch | grep '-' | cut -f 1 -d - | sed -e "s/^\s*//" | sort -u -V | tail -1)"
PREVIOUS_VER="$(git branch | grep '-' | cut -f 1 -d - | sed -e "s/^\s*//" | sort -u -V | tail -2 | head -1)"

for DIFFTOOL in $DIFFTOOL_REQ; do
  echo
  echo "DIFFTOOL: $DIFFTOOL - PreviousVer: $PREVIOUS_VER - LastVer: $LATEST_VER - Old Branch: $old_branch - New Branch: $new_branch"
  echo

  ############ Enable requested Difftool #####################

  if [ ! -f .gitattributes ]; then
  cat > .gitattributes << EOF
*.[jJ][pP][eE][gG] diff=exiftool
*.[jJ][pP][gG] diff=exiftool
#*.[jJ][pP][eE][gG] diff=identitfy-verbose
#*.[jJ][pP][gG] diff=identitfy-verbose
#*.[jJ][pP][eE][gG] diff=identitfy-format
#*.[jJ][pP][gG] diff=identitfy-format
#*.[jJ][pP][eE][gG] diff=exiv2
#*.[jJ][pP][gG] diff=exiv2
EOF
  fi

  case $DIFFTOOL in
    exiftool)
      DIFFTOOL_CMD="exiftool -a -G1 -sort -s -e --Filename --FileAccessDate --FileInodeChangeDate --FileModifyDate --FileCreateDate"
      ;;
    exiv2)
      DIFFTOOL_CMD="exiv2_for_diff.sh"
      ;;
    identitfy-verbose)
      DIFFTOOL_CMD="identify -verbose"
      ;;
    identitfy-format)
      DIFFTOOL_CMD="identify -format '%[EXIF:*]'"
      ;;
  esac

  TEXTCONV_CMD="$(git config --get diff.$DIFFTOOL.textconv)"

  if [ ! a"$DIFFTOOL_CMD" = a"$TEXTCONV_CMD" ]; then
    if [ ! -z "$TEXTCONV_CMD" ]; then
      echo "updating textconv command for $DIFFTOOL"
      # Clean textconv cache
      git update-ref -d refs/notes/textconv/"$DIFFTOOL"
    else
      echo "adding textconv command for $DIFFTOOL"
    fi
    git config --local diff."$DIFFTOOL".textconv "$DIFFTOOL_CMD"
  fi
  git config --local diff."$DIFFTOOL".cachetextconv true

  sed -i -e "/$DIFFTOOL/ ! s/^\*/#*/" -e "/^#*.*$DIFFTOOL/ s/^#//" .gitattributes

  mkdir -p "$ETA_REPO_PATH/tests/output/tags_diff"

  if [ -n "$old_branch" ] && [ -n "$new_branch" ]; then
    run_git_diff "$old_branch" "$new_branch"
  else

    if [ -n "$old_version" ] && [ -n "$new_version" ]; then
      LATEST_VER="$new_version"
      PREVIOUS_VER="$old_version"
    fi

    for engine in exiv2 libexif pixymeta aee; do

      LAST_API="$(git branch | grep "$LATEST_VER-$engine" | cut -f 3 -d - | sort -u -V | tail -n1)"

      if [ -n "$LAST_API" ]; then
        # Check that ETA doesn't alter Exif data
        # Diff: original vs eta (same version)
        B1="$LATEST_VER-original"
        B2="$LATEST_VER-$engine-$LAST_API"
        run_git_diff "$B1" "$B2" original_vs_eta
        echo

        # Check that there is no change between each API
        # Diff: api diff (same version)
        APIS="$(git branch | grep "$LATEST_VER-$engine" | cut -f 3 -d - | sort -u -V)"
        for api in $APIS; do
          if [ -z "$previous_api" ]; then
            previous_api=$api
            continue
          fi
          B1="$LATEST_VER-$engine-$previous_api"
          B2="$LATEST_VER-$engine-$api"
          run_git_diff "$B1" "$B2" api_diff
          previous_api=$api
        done
        echo

        # Check that new ETA version doesn't bring a regression
        # Diff: version diff (latest api of each)
        LAST_API_PREVIOUS="$(git branch | grep "$PREVIOUS_VER-$engine" | cut -f 3 -d - | sort -u -V | tail -n1)"
        B1="$PREVIOUS_VER-$engine-$LAST_API_PREVIOUS"
        B2="$LATEST_VER-$engine-$LAST_API"
        run_git_diff "$B1" "$B2" old_vs_new
        echo
      fi

      unset previous_api

    done
  fi
done
