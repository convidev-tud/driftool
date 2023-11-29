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
import subprocess
import gc
import os
import re
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
    head: str | None = None


    def __init__(self, input_dir, fetch_updates: bool, ignore_files: list[str], ignore_branches: list[str]):
        self._input_dir = input_dir
        self._fetch_updates = fetch_updates
        self._branch_ignores = ignore_branches
        self._file_ignores = ignore_files
        self.branches = list()


    def create_reference_tmp(self):
        self._reference_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._input_dir, self._reference_tmp_path)


    def commit_gitignore_extension(self):
        gitignore = open(self._reference_tmp_path + "/.gitignore", "a")
        for entry in self._file_ignores:
            gitignore.write(entry + "\n")
            gitignore.flush()
        gitignore.close()
        subprocess.run(["git", "add", ".gitgnore"], capture_output=True, cwd=self._reference_tmp_path)
        subprocess.run(["git", "commit", "-m", '"close setup (driftool)"'], capture_output=True, cwd=self._reference_tmp_path)


    def clear_reference_tmp(self):
        try:
            os.access(self._reference_tmp_path, stat.S_IWUSR)
            rmtree(self._reference_tmp_path)
            self._reference_tmp_path = None
        except:
            print("DELETE TMP/ FILE MANUALLY!")


    def create_working_tmp(self):
        self._working_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._reference_tmp_path, self._working_tmp_path)


    def reset_working_tmp(self):
        cancel_merge = subprocess.run(["git", "merge", "--abort"], capture_output=True, cwd=self._working_tmp_path)
        if self.head is not None:
            reset = subprocess.run(["git", "reset", "--hard", self.head], capture_output=True, cwd=self._working_tmp_path)
            #print(reset.stdout)
        stash_clutter = subprocess.run(["git", "stash"], capture_output=True, cwd=self._working_tmp_path)


    def clear_working_tmp(self):
        try:
            os.access(self._working_tmp_path, stat.S_IWUSR)
            rmtree(self._working_tmp_path)
            self._working_tmp_path = None
            self.head = None
        except:
            print("DELETE TMP/ FILE MANUALLY!")



    def materialize_all_branches_in_reference(self) -> list[str]:
        #get the git repository in reference tmp
        path = self._reference_tmp_path

        # checkout each origin branch
        remote_branches_raw = subprocess.run(["git", "branch", "--all"], capture_output=True, cwd=path).stdout.decode("utf-8")
        all_branches: list[str] = list()
        for line in remote_branches_raw.split("\n"):
            line = line.replace("remotes/origin/", "").replace("*", "").replace(" ", "")
            if not line in all_branches and not "HEAD->" in line and not line.isspace() and line != "":
                all_branches.append(line)

        for branch in all_branches:
            print(branch)
            subprocess.run(["git", "checkout", branch], capture_output=True, cwd=path)
            subprocess.run(["git", "stash"], capture_output=True, cwd=path)
            
            if self._fetch_updates:
                pull = subprocess.run(["git", "pull", "origin", branch], capture_output=True, cwd=path).stdout

            self.commit_gitignore_extension()
        
        self.branches = list()
        
        excludes = list()

        for rule in self._branch_ignores:
            excludes.append(re.compile(rule))

        for branch in all_branches:
            ignore = False
            for expr in excludes:
                match = expr.search(branch)
                if match is not None:
                    ignore = True
                    break
            if not ignore:
                self.branches.append(branch)

        self.branches.sort()
        return self.branches
    

    def merge_and_count_conflicts(self, base_branch: str, incoming_branch: str) -> PairwiseDistance:
        
        distance = PairwiseDistance()

        subprocess.run(["git", "checkout", base_branch], capture_output=True, cwd=self._working_tmp_path)
        
       
        m = re.compile("(?<=commit\\s)(.*?)(?=\\\n)")
        commit_log = subprocess.run(["git", "log", "-n 1"], capture_output=True, cwd=self._working_tmp_path).stdout
        commit_hash = m.search(commit_log.decode("utf-8")).group()
        self.head = commit_hash

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
