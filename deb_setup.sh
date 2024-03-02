#!/bin/bash

rm -rf ./volume
rm -rf ./tmp
mkdir -p ./volume

apt-get -y update
apt-get -y upgrade

apt -y install python3
apt -y install python3-venv
apt -y install python-is-python3
apt -y install python3-pip

apt -y install git

git config --global user.name "driftool"
git config --global user.email "analysis@driftool.io"

python3 -m venv env
source env/bin/activate
python3 -m pip install -r requirements.txt