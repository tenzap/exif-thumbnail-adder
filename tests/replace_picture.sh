#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

function remove_extension_from_file {
  local extension BASENAME input
  input="$1"
  extension="$(echo "$input" | awk -F. '{print $NF}')"
  BASENAME=$(echo "${input%.${extension}}")
  echo "${BASENAME}"
  #echo ${input} | sed "s/.${extension}//gI";
}

while read -r picture; do

    R=$((RANDOM % 256))
    G=$((RANDOM % 256))
    B=$((RANDOM % 256))

    extension="$(echo "$picture" | awk -F. '{print $NF}')"
set -x
    if [ ! -e "$(remove_extension_from_file "$picture")-bak.$extension" ]; then
      mv "$picture" "$(remove_extension_from_file "$picture")-bak.$extension"
    fi
    convert "$(remove_extension_from_file "$picture")-bak.$extension" -fill "rgb($R,$G,$B)" -colorize 100 "$picture"
    if [ -e "$(remove_extension_from_file "$picture")-bak.$extension" ]; then
      rm "$(remove_extension_from_file "$picture")-bak.$extension"
    fi

done < <(find . -iname "*.jpg" -not -iname "*-bak.jpg")
