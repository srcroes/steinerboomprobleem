#!/bin/bash
# find all .txt java output files and add .csv files without execution time (only weight)
find . -iname "*.txt" -exec bash -c 'cat "$1" | sed "1d;$d" | sed "s/,/./g" | sed "s/([^()]*)//g" | sed -E "s/(^.)|(.\s*$)//g" | sed "/^\s/!d" | sed "s/\s//g" | sed "s/│/, /g" | sed "1s/^/file/" > "${1%.txt}_w".csv' - '{}' \;
