#!/bin/bash

# $1 is the relative path to the input directory.
# $2 is the relative path to the working directory.
# $3 is the relative path to the configuration file within the input directory.

# $4 is the relative path to the repository within the input directory.
# $5 is the relative path to the output directory within the input directory.

echo "gradle run ${PWD}/$1 ${PWD}/$2 $3 -i $4 -o $5 -m git -t 4"
gradle run --args="${PWD}/$1 ${PWD}/$2 $3 -i $4 -o $5 -m git -t 4"