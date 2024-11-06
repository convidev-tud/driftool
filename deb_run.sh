#!/bin/bash

# $1 is the location of the repository config as 

echo config file: $1

#echo contents of volume/
cd volume
ls -l

cd ..

echo the user is: "$USER"

echo create ramdisk with $2 GB size
mkdir -p ./tmp
sudo chmod 777 ./

# Comment the following two lines to disable the ramdisk creation.
# This reduces the RAM usage but increases the analysis time.
sudo mount -v -t tmpfs -o size=$(($2))G ramdisk ./tmp
mount | tail -n 1

echo preparing analysis
ls -l

source env/bin/activate

python -m build
pip3 install ./dist/driftool-0.0.1-py3-none-any.whl --no-build-isolation

python3 -W ignore driftool/main.py $1