#!/bin/bash

# $1 is the repository directoy path within the volume folder.

# $2 is the output directory path within the volume folder.

# $3 is the location of the repository config within the volume folder.

# $4 is the size of the ramdisk the container is allowed to create in GB.
# The container has a base RAM requirement of around 2GB. 
# For multithreaded analysis, the RAM requirement is (x + 2x * threads) where x is the size of the repository.
# FOr example, for a 2GB repository analyzed on 8 threads, the RAM requirement is minimum 18GB. To ensure a smooth run, 24GB would be ideal.

# $5 is the number of threads to use for the analysis.

# $6 is the mode of analysis = git|matrix|....

echo repo path in volume: $1
echo output path in volume: $2
echo repo config path in volume: $3
echo ramdisk size: $4
echo number of threads: $5
echo mode: $6

#echo contents of volume/
cd volume
ls -l

cd ..

echo the user is: "$USER"

echo create ramdisk with $4 GB size
mkdir -p ./tmp
sudo chmod 777 ./

# Comment the following two lines to disable the ramdisk creation.
# This reduces the RAM usage but increases the analysis time.
sudo mkdir /dtmp/
sudo mount -v -t tmpfs -o size=$(($4))G ramdisk /dtmp
mount | tail -n 1

echo preparing analysis
ls -l

sudo ./driftool_kt-1.0-SNAPSHOT/bin/driftool_kt '/driftool/volume' '/dtmp' $3  -i $1 -o $2 -m $6 -t $5

