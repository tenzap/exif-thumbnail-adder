#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Expected differences:
#  - Flavor: standard only
#     Between API29 & API30, starting from API30: the logs will show "Could not copy file timestamps & attributes: java.nio.file.AccessDeniedException"
#     Between API29 & API30, starting from API30: on test withoutSAF: Will show "ERROR moving document:" Maybe TODO?

APIS="26 27 28 29 30 31 32 33"

for REF in $APIS; do
  for API in $APIS; do
    if [ $REF -ge $API ]; then
      continue
    fi
    echo "REF:$REF vs API:$API"
    FILES_REF=$(cd "output/test_output/$REF" && find . -name log.txt -or -name sync_log.txt)
    FILES_API=$(cd "output/test_output/$API" && find . -name log.txt -or -name sync_log.txt)
    ALL_FILES=$(echo -e "${FILES_API}\n${FILES_REF}" | sort -u)
    {
      for file in $ALL_FILES; do
        if ! diff -q "output/test_output/$REF/$file" "output/test_output/$API/$file" > /dev/null; then
          echo "File: $file"
          diff -Naur "output/test_output/$REF/$file" "output/test_output/$API/$file"
        fi

        # Add additional comparison: API29 vs API30WithAllFilesAccess
        # This permits to see the remaining differences when the "Could not copy file timestamps..." differences are not there
        if [ "$REF" -le 29 ] && [ "$API" -ge 30 ]; then
          if [[ $file =~ .*_addThumbsSettingsDefault\/ ]]; then
            file2=${file/_addThumbsSettingsDefault\//_addThumbsSettingsAllFilesAccessOn\/}
            sed -e "s#_addThumbsSettingsDefault#PLACEHOLDER#" "output/test_output/$REF/$file" > /tmp/f1
            sed -e "s#_addThumbsSettingsAllFilesAccessOn#PLACEHOLDER#" "output/test_output/$API/$file2" > /tmp/f2
            if ! diff -q "/tmp/f1" "/tmp/f2" > /dev/null; then
              echo "File: $file vs $file2"
              diff -Naur "/tmp/f1" "/tmp/f2"
            fi
            rm "/tmp/f1" "/tmp/f2"
          fi
        fi
      done
    } | tee "output/diff-$REF-$API.diff"
  done
done
