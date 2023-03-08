#!/bin/bash

# SPDX-FileCopyrightText: 2023 Fab Stz <fabstz-it@yahoo.fr>
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Usage
#   ./scripts/unzip_github_output_artifact.sh artifact.zip
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

unzip -q -c "$1" | tar -C "$ETA_REPO_PATH/tests/output/" --strip-components=2 -xvf -
