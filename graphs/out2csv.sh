#!/bin/bash
[ $# -ge 1 -a -f "$1" ] && input="$1" || input="-"
# change java output txt to csv
cat $input | sed '1d;$d' | sed 's/,/./g' | sed -E 's/(^.)|(.\s*$)//g' | sed '/^\s/!d' | sed 's/\s//g' | sed 's/│/, /g' | sed '1s/^/file/'
