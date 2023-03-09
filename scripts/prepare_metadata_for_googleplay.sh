#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Google Play has some expectations for the metadata
#
# - title.txt - [App name] (30 chars)
# - short_description.txt (80 chars)
# - full_description.txt (4000 chars)
#
# title, short_description & full_description must be in all languages
# So copy them from en_US to each language temporarily
#
#
# Languages:
#  - language code should be same as in the google play console
#    - vi-VN doesn't exist. Use 'vi' instead (THIS HAS BEEN UPDATED in CROWDIN & the repo directly)
#
#
# Max 8 screenshots for each: smartphone, tablet 7", tablet 10"
#

set -e

truncate_to_chars() {
  local f="$1"
  local count="$2"

  perl -Mopen=locale -ne '
  BEGIN{$/ = \'$count'} truncate STDIN, tell STDIN; last' <> "$f"

}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

PROJECT_ROOT="$(realpath "$SCRIPT_DIR/..")"

echo "Preparing metadata for submission to Google Play"

MODIFIED_FILES_IN_METADATA=$(git status -s -- "$PROJECT_ROOT/fastlane/metadata" | wc -l)

if [ "$MODIFIED_FILES_IN_METADATA" -ne 0 ]; then
  echo "There are modified files in fastlane/metadata:"
  git status -s -- "$PROJECT_ROOT/fastlane/metadata"
  exit 1
fi

for metadata in title.txt short_description.txt full_description.txt; do

  # Truncate metadata to max char count
  if [ "$metadata" = "title.txt" ]; then
    max_count=30
  elif [ "$metadata" = "short_description.txt" ]; then
    max_count=80
  elif [ "$metadata" = "full_description.txt" ]; then
    max_count=4000
  fi
  for file in $(find "$PROJECT_ROOT/fastlane/metadata" -name "$metadata"); do
    truncate_to_chars "$file" "$max_count"
  done

  # Add missing metadata
  REF_LANG="en-US"
  for language in $(cd "$PROJECT_ROOT/fastlane/metadata/android" && find . -mindepth 1 -maxdepth 1 -type d); do
    if [ ! -f "$PROJECT_ROOT/fastlane/metadata/android/$language/$metadata" ]; then
      cp -a "$PROJECT_ROOT/fastlane/metadata/android/$REF_LANG/$metadata" "$PROJECT_ROOT/fastlane/metadata/android/$language/"
    fi
  done

done

# Copy changelog
VerCode="$(cut -d'+' -f2 < "$PROJECT_ROOT/version_last_tag.txt")"
REF_LANG="en-US"
truncate_to_chars "$PROJECT_ROOT/fastlane/metadata/android/$REF_LANG/changelogs/$VerCode.txt" "500"

for language in $(cd "$PROJECT_ROOT/fastlane/metadata/android" && find . -mindepth 1 -maxdepth 1 -type d); do
  if [ ! -f "$PROJECT_ROOT/fastlane/metadata/android/$language/changelogs/$VerCode.txt" ]; then
    mkdir -p "$PROJECT_ROOT/fastlane/metadata/android/$language/changelogs/"
    cp -a "$PROJECT_ROOT/fastlane/metadata/android/$REF_LANG/changelogs/$VerCode.txt" "$PROJECT_ROOT/fastlane/metadata/android/$language/changelogs/"
  fi
done

# Show modifications
MODIFIED_FILES_IN_METADATA=$(git status -s -- "$PROJECT_ROOT/fastlane/metadata" | wc -l)
if [ "$MODIFIED_FILES_IN_METADATA" -ne 0 ]; then
  echo "These files were modified files in fastlane/metadata:"
  git diff -- "$PROJECT_ROOT/fastlane/metadata"
fi

# Rename language dirs
# mv "$PROJECT_ROOT/fastlane/metadata/android/vi-VN" "$PROJECT_ROOT/fastlane/metadata/android/vi"
