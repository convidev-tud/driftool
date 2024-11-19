#!/bin/bash

# $1 is the relative path to the input directory.
# $2 is the relative path to the working directory.
# $3 is the relative path to the configuration file within the input directory.
# $4 is the relative path to the output directory within the input directory.


gradle run --args="absolute_input absolute_working_dir config.yaml -i repo -m git -t 4"