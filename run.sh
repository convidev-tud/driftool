#!/bin/bash

# $1 is the repository directoy path within the volume folder.

# $2 is the output directory path within the volume folder.

# $3 is the location of the repository config within the volume folder.

# $4 is the size of the ramdisk the container is allowed to create in GB.
# The container has a base RAM requirement of around 2GB. 
# For multithreaded analysis, the RAM requirement is (x + 2x * threads) where x is the size of the repository.
# FOr example, for a 2GB repository analyzed on 8 threads, the RAM requirement is minimum 18GB. To ensure a smooth run, 24GB would be ideal.

# $5 is the number of threads to use for the analysis.

# $6 is the mode of analysis = "git|matrix|...".

docker run -it --rm --privileged --name driftool --mount type=bind,source="$(pwd)"/volume,target=/driftool/volume driftool:latest "$1" "$2" "$3" "$4" "$5" "$6"