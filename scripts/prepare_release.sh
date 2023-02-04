#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/prepare_release.sh
#
# This script will:
#  - prepare the release commit
#     - release notes/changelog
#     - screenshots
#     - set a fixed version for versionCode & versionNumber in build.gradle (required by F-Droid)
#  - create a tag with the version
#  - create a commit that reverts to automatic versionCode and versionName
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

PREVIOUS_VER="$(git describe --abbrev=0)"

echo "Enter desired version name (eg. last version was: $PREVIOUS_VER)"
read -r

VER="${REPLY}"
echo
echo -n "Creating tag for version $VER..."

git tag -a -m "release $VER" "$VER"
echo " Done."

echo
echo "Sync gradle in Android Studio, then press Enter"
read -r

VerCode="$(cut -d'+' -f2 < version_last_tag.txt)"

echo
echo "Set this versions in build.gradle: $((VerCode+1)) (no need to sync gradle), then press Enter"
read -r

echo
echo "Creating changelog file: ($((VerCode+1)).txt). Press Enter to start editing it."
PREV_CHANGELOG="$(ls -1 fastlane/metadata/android/en-US/changelogs/ | sort -h | grep -v "$((VerCode+1)).txt" | tail -1)"
if [ ! -f "fastlane/metadata/android/en-US/changelogs/$((VerCode+1)).txt" ]; then
  git log "$PREVIOUS_VER".."$VER" --pretty="- %s" > "fastlane/metadata/android/en-US/changelogs/$((VerCode+1)).txt"
  echo -e "\n\n\n---\n" >> "fastlane/metadata/android/en-US/changelogs/$((VerCode+1)).txt"
  cat "fastlane/metadata/android/en-US/changelogs/$PREV_CHANGELOG" >> "fastlane/metadata/android/en-US/changelogs/$((VerCode+1)).txt"
fi
editor "fastlane/metadata/android/en-US/changelogs/$((VerCode+1)).txt"

echo "Press Enter to continue"
read -r

git tag -d "$VER"
git commit -a -m "release $VER"


ret=0
"$SCRIPT_DIR/screenshots.sh" ask || ret=$?

if [ $ret -eq 0 ]; then
  echo "Screenshots created successfully."
elif [ $ret -eq 51 ]; then
  echo "Screenshots could NOT be created. To retry launch
   $SCRIPT_DIR/screenshots.sh

Then press enter here."
  read -r
elif [ $ret -eq 52 ]; then
  echo "Continuing without screenshots"
else
  echo "==========================
Screenshots may not be created or fully created. Check, otherwise, relaunch:
   $SCRIPT_DIR/screenshots.sh

To not update screenshots with this release, please run this before continuing here:

   git checkout HEAD fastlane/metadata/android/**/images/phoneScreenshots/*
   git checkout fastlane/metadata/android/screenshots.html

Then check again that no screenshots will be changed

   git status

To stop this process here and rollback to last commit, run:

   git reset --soft HEAD^
   git restore --staged app/build.gradle version_last_tag.txt
   git checkout fastlane app/build.gradle version_last_tag.txt
   git status

Then press enter here."
  read -r
fi

# To update version_last_tag.txt
git tag -a -m "release $VER" "$VER"
echo
echo "Sync gradle in Android Studio, then press Enter"
read -r
git tag -d "$VER"

git add fastlane/metadata/**
#sed s/+$VerCode$/+$((VerCode+1))/ version_last_tag.txt
git commit -a --amend -m "release $VER"
git tag -a -m "release $VER" "$VER"

git reflog expire --expire=90.days.ago --expire-unreachable=now --all

echo
echo "Change build.gradle: revert to automatic versionCode and versionName, sync gradle, then press Enter"
read -r

git add app/build.gradle
git commit -m "revert to automatic versionCode and versionName"

echo
echo "Do you want to git push master & tags? [Yn]"
read -r
if [ -z "$REPLY" ] || [[ "$REPLY" =~ [yY] ]]; then
  echo "Proceeding..."
  git push origin master
  git push --tags
else
  echo "Skipped."
fi

echo
echo "Do you want to build signed release (APK & AAB) & optionally publish to google play? [Yn]"
read -r
if [ -z "$REPLY" ] || [[ "$REPLY" =~ [yY] ]]; then
  echo "Proceeding..."
  git checkout "$VER"
  bundle exec fastlane build_release
  mkdir -p 0_personal/signed_releases
  mv -n "app/build/outputs/apk/standard/release/"*.apk 0_personal/signed_releases
  mv -n "app/build/outputs/bundle/standardRelease/"exifthumbnailadder-*-standard-release.aab 0_personal/signed_releases
  echo
  echo "Signed APK & AAB are in: 0_personal/signed_releases"
  echo

  echo
  echo "Do you want to publish to google play? [Yn]"
  read -r ggp
  if [ -z "$ggp" ] || [[ "$ggp" =~ [yY] ]]; then
    echo "Proceeding..."
    bundle exec fastlane deploy
  else
    echo "Skipped."
  fi

else
  echo "Skipped."
fi

if [ ! "$(git branch --show-current)" = "master" ]; then
  echo
  echo "Do you want to return to master branch (currently on $(git branch --show-current))?  [Yn]"
  read -r
  if [ -z "$REPLY" ] || [[ "$REPLY" =~ [yY] ]]; then
    echo "Proceeding..."
    git checkout master
  else
    echo "Skipped."
  fi
fi

echo
echo "Script finished."
echo
