#!/bin/bash

echo config file: $1

#echo contents of volume/
cd volume
ls -l

cd ..

echo the user is: "$USER"

echo create ramdisk with $2 GB size
mkdir -p ./tmp
sudo chmod 777 ./
sudo mount -v -t tmpfs -o size=$(($2))G ramdisk ./tmp
mount | tail -n 1

echo preparing analysis
ls -l

source env/bin/activate
python3 -W ignore main.py $1