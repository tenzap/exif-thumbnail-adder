#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/store_output_to_pic_repo.sh
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

cd "$PICS_REPO_PATH"

git checkout master > /dev/null

PREVIOUS_VER="$(git branch | grep '-' | cut -f 1 -d - | sed -e "s/^\s*//" | sort -u -V | tail -1)"
echo -n "Enter version name (eg. last version was '$PREVIOUS_VER'): "
read -r

if [ -z "$REPLY" ]; then
  VERSION="$PREVIOUS_VER"
else
  VERSION="${REPLY}"
fi

PREVIOUS_API="$(git branch | grep '-' | cut -f 3 -d - | sed -e "s/^\s*//" | sort -u -V | tail -1)"
echo -n "Enter API (eg. last API was '$PREVIOUS_API'): "
read -r
if [ -z "$REPLY" ]; then
  API="$PREVIOUS_API"
else
  API="${REPLY}"
fi

echo
echo "VERSION: $VERSION - API: $API"
echo

############ UPDATE MASTER BRANCH #####################

git checkout master > /dev/null

if [ ! -z "$(git status --porcelain > /dev/null)" ]; then
  # Uncommitted changes
  echo "branch 'master': Uncommitted changes"
  exit 1
fi

if ! git show-ref --quiet refs/heads/"$VERSION"-original; then
  git checkout -b "$VERSION-original"
else
  git checkout "$VERSION-original" > /dev/null
fi

cp -a "$ETA_REPO_PATH/tests/output/test_pics/"* "$PICS_REPO_PATH/jpg"

# Restore exec bit on 2 files (otherwise, the files would be considered different)
chmod +x jpg/Reconyx_HC500_Hyperfire.jpg \
  jpg/xmp/BlueSquare.jpg

# Add new files (untracked files)
git status --porcelain=1 | grep "^??" | cut -d " " -f 2- | xargs -r git add
if git status --porcelain=1 | grep "^A "; then
  git commit -m "add additional original pictures used in ETA $VERSION"
fi

# Check there aren't modified files. If there are, it should be fixed.
if git status --porcelain=1 | grep -v "^??"; then
  echo "There are Uncommitted changes. Please fix manually."
  exit 1
fi

############ ITERATE ON EACH ENGINE #####################

for engine in exiv2 libexif pixymeta AEE; do
#for engine in exiv2; do
  case $engine in
  exiv2)
    ENGINEID=Exiv2
    ENGINEREPO="${ENGINEID,,}" # = lowercase
    ;;
  libexif)
    ENGINEID=LibExif
    ENGINEREPO="${ENGINEID,,}" # = lowercase
    ;;
  pixymeta)
    ENGINEID=Pixymeta
    ENGINEREPO="${ENGINEID,,}" # = lowercase
    ;;
  AEE)
    ENGINEID=AEE
    ENGINEREPO="${ENGINEID,,}" # = lowercase
    ;;
  esac

  BRANCH="$VERSION-$ENGINEREPO-$API"

  if ! git show-ref --quiet refs/heads/"$BRANCH"; then
    git checkout -b "$BRANCH"
  else
    echo "branch $BRANCH already exists. Continuing with next engine."
    continue
  fi

  TESTDIR="$ETA_REPO_PATH/tests/output/test_output/$API/standard_addThumbsSettings${ENGINEID}/DCIM/test_standard_addThumbsSettings${ENGINEID}"

  if [ ! -e "$TESTDIR" ]; then
    continue
  fi

  FILES=$(find "$ETA_REPO_PATH/tests/output/test_pics/" -type f | sort | sed -e "s|$ETA_REPO_PATH/tests/output/test_pics/||")

  for file in $FILES; do
    cp -a "$TESTDIR/$file" "$PICS_REPO_PATH/jpg/$file"
    git add "$PICS_REPO_PATH/jpg/$file"
  done

  if git status --porcelain=1 | grep "^[AM] "; then
    git commit -m "Import output of ETA $VERSION, with engine $ENGINEREPO, run on API $API"
  fi

  # Check there aren't uncommitted files. If there are, it should be fixed.
  if git status --porcelain=1 | grep -v "^[AM] "; then
    echo "There are Uncommitted changed. Please fix manually."
    exit 1
  fi
done

git checkout master > /dev/null
