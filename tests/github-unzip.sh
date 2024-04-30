#!/bin/bash

zipfile="$1"
outdir="$(dirname "$(realpath "$1")")"

funzip "$zipfile" | tar -x --exclude tests/output/test_pics -C "$outdir" --strip-components=3
