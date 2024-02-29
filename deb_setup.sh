#!/bin/bash

apt-get update
apt-get upgrade

apt install python3
apt install python3-virtualenv
apt install python-is-python3

apt install git

git config user.name "driftool"
git config user.email "analysis@driftool.io"

python -m venv env
source env/bin/activate
python -m pip install -r requirements.txt