#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/screenshots.sh [ask]
#
#  Use 'ask' to prompt user if screenshots are desired.
#
# Exit code:
#   - 0: screenshots produced
#   - 51: missing env var
#   - 52: screenshots not produced (skipped by user)
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

if [ "$1" = "ask" ]; then
  echo "Do you want to create the screenshots? [Yn]"

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

bundle exec fastlane screenshots
