#!/bin/bash

docker run -it --rm --privileged --name driftool --mount type=bind,source="$(pwd)"/volume,target=/driftool/volume driftool:latest "$1" "$2"