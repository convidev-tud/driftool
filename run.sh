#!/bin/bash

# $1 is the location of the repository config. 
# The config must be located in a folder called volume in the same directory as this script.
# The path must start with /volume/.../x.yaml
# The repository and output folder must also be children of the volume folder!

# $2 is the size of the ramdisk the container is allowed to create in GB.
# The container has a base RAM requirement of around 2GB. 
# For multithreaded analysis, the RAM requirement is (x + 2x * threads) where x is the size of the repository.
# FOr example, for a 2GB repository analyzed on 8 threads, the RAM requirement is minimum 18GB. To ensure a smooth run, 24GB would be ideal.

# $3 is the number of threads to use for the analysis.

docker run -it --rm --privileged --name driftool --mount type=bind,source="$(pwd)"/volume,target=/driftool/volume driftool:latest "$1" "$2" "$3"