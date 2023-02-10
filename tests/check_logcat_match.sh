#!/bin/bash

#
# Search if the logcat.txt contains the IndexOutOfBoundsException
#
#

MATCH="IndexOutOfBoundsException: setSpan"

grep "$MATCH" output/screenrecords/*/*.logcat.txt
RET=$?

if [ $RET -eq 0 ]; then
  echo
  echo
  echo "Logcat matches: [$MATCH]"
  exit 1
else
  echo "No match found for: [$MATCH]"
fi
