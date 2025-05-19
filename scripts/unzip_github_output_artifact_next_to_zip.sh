#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/unzip_github_output_artifact_next_to_zip.sh artifact.zip
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

if [ -z "$1" ]; then
    echo "Usage:"
    echo "  ./scripts/unzip_github_output_artifact_next_to_zip.sh artifact.zip"
    echo
    exit 1
fi

zipfile="$1"
outdir="$(dirname "$(realpath -s "$1")")"

funzip "$zipfile" | tar -x --exclude tests/output/test_pics -C "$outdir" --strip-components=3
