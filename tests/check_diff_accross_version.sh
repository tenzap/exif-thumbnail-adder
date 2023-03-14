#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if [ "$PWD" = "$SCRIPT_DIR" ]; then
    echo "Script is called from $PWD: OK!"
else
    echo
    echo "Must be run from project tests dir: ./$( basename "${BASH_SOURCE[0]}" )"
    echo
    exit 1;
fi

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage:"
  echo "  ./check_diff_accross_version.sh <old_output_dir> <new_output_dir>"
  echo
  exit 1
fi

OLD="$(realpath "$1")"
NEW="$(realpath "$2")"

FILES="$(cd "$2" && find . -name log.txt -or -name sync_log.txt | sort)"

relpath_old="$(sed -e "s#/#_#g" <<< "$OLD")"
relpath_new="$(sed -e "s#/#_#g" <<< "$NEW")"

rm -f "old_vs_new/$relpath_old=$relpath_new.diff"
for file in $FILES; do
  mkdir -p "output/old_vs_new"
  diff -Naur "$OLD/$file" "$NEW/$file" >> "output/old_vs_new/$relpath_old=$relpath_new.diff"
done
