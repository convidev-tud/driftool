#  Copyright 2023 Karl Kegel
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import os, os.path
import re

def purge_blacklist(regex_list: list[str], root_path: str):

    git_pattern = re.compile(".git")

    for regex in regex_list:
        pattern = re.compile(regex)
        for root, dirs, files in os.walk(root_path):

            if git_pattern.search(str(root)) is None:
                print("TRAVERSE: " + str(root) + " " + str(dirs) + " " + str(files))

                for file in files:
                    if pattern.search(root + file) is not None:
                        os.remove(os.path.join(root, file))


def keep_whitelist(regex_list: list[str], root_path: str):

    git_pattern = re.compile(".git")
    patterns = list()

    for regex in regex_list:
        pattern = re.compile(regex)
        patterns.append(pattern)

    for root, dirs, files in os.walk(root_path):

        if git_pattern.search(str(root)) is None:
            print("TRAVERSE: " + str(root) + " " + str(dirs) + " " + str(files))

            for file in files:
                do_purge = True
                for pattern in patterns:
                    if pattern.search(root + file) is not None:
                        do_purge = False
                        break;
                if do_purge:
                    os.remove(os.path.join(root, file))