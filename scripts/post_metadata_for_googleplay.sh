#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Google Play has some expectations for the metadata.
# This script restores to the state before the "prepare"
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

PROJECT_ROOT="$(realpath "$SCRIPT_DIR/..")"

echo "Postparing metadata after submission to Google Play"

# Rename language dirs
#if [ -d "$PROJECT_ROOT/fastlane/metadata/android/vi" ]; then
#  mv "$PROJECT_ROOT/fastlane/metadata/android/vi" "$PROJECT_ROOT/fastlane/metadata/android/vi-VN"
#fi

# Restore modified files to previous state
git checkout "$PROJECT_ROOT/fastlane/metadata/"

# Cleanup created files
TO_DELETE="$(cd "$PROJECT_ROOT/" && git status -s -- "$PROJECT_ROOT/fastlane/metadata/" | grep "^??" | cut -f 2 -d " ")"
for file in $TO_DELETE; do
  if [ -f "$PROJECT_ROOT/$file" ]; then
    rm "$PROJECT_ROOT/$file"
  fi
done

# Cleanup changelog
VerCode="$(cut -d'+' -f2 < "$PROJECT_ROOT/version_last_tag.txt")"
for language in $(cd "$PROJECT_ROOT/fastlane/metadata/android" && find . -mindepth 1 -maxdepth 1 -type d); do
  if [[ "$language" =~ .*en-US ]]; then
    continue
  fi
  if [ -f "$PROJECT_ROOT/fastlane/metadata/android/$language/changelogs/$VerCode.txt" ]; then
    rm "$PROJECT_ROOT/fastlane/metadata/android/$language/changelogs/$VerCode.txt"
  fi
done

# Cleanup changelog dir
TO_DELETE="$(cd "$PROJECT_ROOT/" && git status -s -- "$PROJECT_ROOT/fastlane/metadata/" | grep "^??" | cut -f 2 -d " ")"
for file in $TO_DELETE; do
  if [ -d "$PROJECT_ROOT/$file" ] && [ -z "$(ls -A "$PROJECT_ROOT/$file")" ]; then
    rmdir "$PROJECT_ROOT/$file"
  fi
done
