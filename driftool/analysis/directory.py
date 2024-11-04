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
import shutil
import stat
import subprocess

def purge_blacklist(regex_list: list[str], root_path: str, remove_hidden: bool, log: list[str]):

    git_pattern = re.compile("\.git")

    for regex in regex_list:
        pattern = re.compile(regex)
        for root, dirs, files in os.walk(root_path):

            if git_pattern.search(str(root)) is None:
                #print("TRAVERSE: " + str(root) + " " + str(dirs) + " " + str(files))

                for file in files:
                    if pattern.search(root + file) is not None:
                        os.remove(os.path.join(root, file))

    if remove_hidden:
        items = os.listdir(root_path)
        for item in items:
            item_path = os.path.join(root_path, item)
            if item.startswith('.') and git_pattern.search(str(item_path)) is None :
                if os.path.isdir(item_path):
                    shutil.rmtree(item_path)
                else:
                    os.remove(item_path)



def keep_whitelist(regex_list: list[str], root_path: str, remove_hidden: bool, log: list[str]):

    try:
        git_pattern = re.compile("\.git")
        patterns = list()

        for regex in regex_list:
            pattern = re.compile(regex)
            patterns.append(pattern)

        match_count = 0
        symlink_count = 0

        os.access(root_path, stat.S_IWUSR)

        for root, dirs, files in os.walk(root_path, topdown=True):

            if git_pattern.search(str(root)) is None:
                #print("TRAVERSE: " + str(root) + " " + str(dirs) + " " + str(files))

                for file in files:

                    do_purge = True
                    full_path = os.path.join(root, file)

                    if os.path.islink(full_path):
                        os.unlink(full_path)
                        symlink_count += 1
                        continue

                    for pattern in patterns:
                        if pattern.search(file) is not None:
                            do_purge = False
                            break;
                   
                    if do_purge:
                        os.remove(full_path)
                        match_count += 1

        if remove_hidden:
            log.append("Attempt delete hidden files")
            items = os.listdir(root_path)
            for item in items:
                item_path = os.path.join(root_path, item)
                if item.startswith('.') and not item == ".git" :
                    log.append("REMOVE HIDDEN: " + item)
                    if os.path.isdir(item_path):
                        shutil.rmtree(item_path)
                    else:
                        os.remove(item_path)


        print("PURGE " + str(match_count) + " FILES")
        print("SYMLK " + str(symlink_count) + " FILES")
        log.append("PURGE " + str(match_count))
        log.append("SYMLK " + str(symlink_count))

    except Exception as e:
        log.append("Exception during whitelist processing")
        log.append(str(e))
        raise e
    

def count_files(root_path: str) -> int:
    total = 0
    for root, dirs, files in os.walk(root_path):
        total += len(files)
    return total


def copy_dir(working_dir: str, source: str, target: str):
    subprocess.run(["mkdir", "-p", "foo/bar/baz"], cwd=working_dir)
    return subprocess.run(["cp", "-r", source, target], capture_output=True, cwd=working_dir)

