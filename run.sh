#!/bin/bash

docker run -d \
  -it \
  --name driftool \
  --mount type=bind,source="$(pwd)"/volume,target=/driftool/volume \
  driftool "$1"