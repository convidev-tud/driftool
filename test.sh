#!/bin/bash

chmod 733 tests/resources/repositories/repo_a_setup.sh
chmod 733 tests/resources/repositories/repo_a_destruct.sh

python -m build
pip install ./dist/driftool-0.0.1-py3-none-any.whl

python -W ignore -m unittest discover