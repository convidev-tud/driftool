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

from shutil import copytree, rmtree
import uuid
import git
import subprocess
import gc
import math
import os
import stat

from lib.data.pairwise_distance import PairwiseDistance

class RepositoryHandler:

    _input_dir: str
    _fetch_updates: bool 
    _reference_tmp_path : str | None
    _working_tmp_path: str | None
    _branch_ignores: list[str]
    _file_ignores: list[str]
    
    branches: list[str]


    def __init__(self, input_dir, fetch_updates: bool, ignore_files: list[str], ignore_branches: list[str]):
        self._input_dir = input_dir
        self._fetch_updates = fetch_updates
        self._branch_ignores = ignore_branches
        self._file_ignores = ignore_files
        self.branches = list()


    def create_reference_tmp(self):
        self._reference_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._input_dir, self._reference_tmp_path)
        
        #TODO create file ignore .gitignore


    def clear_reference_tmp(self):
        os.access(self._reference_tmp_path, stat.S_IWUSR)
        rmtree(self._reference_tmp_path)
        self._reference_tmp_path = None


    def create_working_tmp(self):
        self._working_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._reference_tmp_path, self._working_tmp_path)


    def clear_working_tmp(self):
        gc.collect()
        self.repository.git.clear_cache()
        os.access(self._working_tmp_path, stat.S_IWUSR)
        rmtree(self._working_tmp_path)
        self._working_tmp_path = None


    def materialize_all_branches_in_reference(self) -> list[str]:
        #get the git repository in reference tmp
        path = self._reference_tmp_path
        self.repository = git.Repo(path)

        # checkout each origin branch
        remote_branches = self.repository.remote().refs
        for remote in remote_branches:
            print(remote.name)
            current_branch = remote.name.replace("origin/", "")
            self.repository.git.checkout(current_branch)
            self.repository.git.stash()
            if self._fetch_updates:
                self.repository.remotes.origin.pull()

        available_branches = [h.name for h in self.repository.branches]
        
        self.branches = list()
        for branch in available_branches:
            add = True
            for exception in self._branch_ignores:
                if exception in branch:
                    add = False
                    break
            if add:
                self.branches.append(branch)

        gc.collect()
        self.repository.git.clear_cache()

        return self.branches
    

    def merge_and_count_conflicts(self, base_branch: str, incoming_branch: str) -> PairwiseDistance:
        
        distance = PairwiseDistance()

        self.repository = git.Repo(self._reference_tmp_path)
        self.repository.git.checkout(base_branch)

        stdout_diff = subprocess.run(["git", "diff", base_branch + ".." + incoming_branch], capture_output=True, cwd=self._working_tmp_path).stdout.splitlines()
        diff_size = 0
        for line in stdout_diff:
            line_str = str(line)
            if line_str.startswith("b'+") or line_str.startswith("b'-"):
                diff_size += 1

        if diff_size > 0:
            distance.diff_lines = diff_size
        else:
            distance.diff_lines = 0

        stdout_merge = subprocess.run(["git", "merge", incoming_branch], capture_output=True, cwd=self._working_tmp_path).stdout

        stdout_lines = map(lambda t: str(t), stdout_merge.splitlines())
        conflict_lines = list(filter(lambda s: ("Merge conflict in" in s), stdout_lines))
        conflict_files = list(map(lambda u: u.split("Merge conflict in ")[1], conflict_lines))
        conflict_files = list(map(lambda u: u[:len(u)-1], conflict_files))

        distance.conflicting_files = len(conflict_files)

        if len(conflict_files) > 0:

            #print(">> MERGE FROM " + incoming_branch + " INTO " + base_branch, end='\r')
            sum_of_conflicts = 0
            number_of_conflicts = 0

            for file in conflict_files:
                #print("CONFLICT IN: " + file)

                try:
                    conflicting_file = open(self._working_tmp_path + "/" + file, "r", encoding='utf-8', errors='ignore').readlines()
                except:
                    continue

                inside_conflict = False
                conflict_start_line = 0
                line_index = 0

                for line in conflicting_file:

                    if "<<<<<<<" in line and not inside_conflict:
                        # merge conflict start
                        inside_conflict = True
                        number_of_conflicts += 1
                        conflict_start_line = line_index

                    if ">>>>>>>" in line and inside_conflict:
                        # merge conflict end
                        inside_conflict = False
                        sum_of_conflicts += (line_index - conflict_start_line)

                    line_index += 1

                #print("conflicting lines: " + str(sum_of_conflicts))
                distance.conflicting_lines = sum_of_conflicts
                distance.conficts = number_of_conflicts
        
        return distance
