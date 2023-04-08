#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/screenshots.sh [--ask] [--type=phone|sevenInch]
#
#  Use 'ask' to prompt user if screenshots are desired.
#
# Exit code:
#   - 0: screenshots produced
#   - 51: missing env var
#   - 52: screenshots not produced (skipped by user)
#   - 52: screenshots type invalid or not specified
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

for i in "$@"; do
  case $i in
    --ask)
      ASK=1
      shift
      ;;
    -t=*|--type=*)
      SCREENSHOTS_TYPE="${i#*=}"
      shift
      ;;
    -*)
      echo "Unknown option $i"
      exit 1
      ;;
    *)
      ;;
  esac
done

if [ "$PWD/scripts" = "$SCRIPT_DIR" ]; then
    echo "Script is called from $PWD: OK!"
else
    echo
    echo "Must be run from project root dir: ./scripts/$( basename "${BASH_SOURCE[0]}" )"
    echo
    exit 1;
fi

if [ a"$ASK" = a"1" ]; then
  echo "Do you want to create the screenshots for '$SCREENSHOTS_TYPE'? [Yn]"

  read -r

  if [ -z "$REPLY" ] || [[ "$REPLY" =~ [yY] ]]; then
    echo "Proceeding..."
  else
    echo "Skipped."
    exit 52
  fi
fi

if [ -n "$ANDROID_SDK_ROOT" ]; then
  echo "ANDROID_SDK_ROOT is set to: $ANDROID_SDK_ROOT"
elif [ -e "$HOME/Android/Sdk/" ]; then
  export ANDROID_SDK_ROOT="$HOME/Android/Sdk/"
else
  echo "ANDROID_SDK_ROOT not found. Please set ANDROID_SDK_ROOT environment variable."
  exit 51
fi

echo -e "\nPlease launch the emulator to take screenshots for '$SCREENSHOTS_TYPE'. Then press Enter."
read -r

case $SCREENSHOTS_TYPE in
  phone)
    echo "Producing screenshots for 'phone'."
    bundle exec fastlane screenshots
    ;;
  sevenInch)
    echo "Producing screenshots for 'sevenInch'."
    bundle exec fastlane screenshots_sevenInch
    ;;
  *)
    echo "Screenshot type invalid or not specified."
    exit 53
    ;;
esac
