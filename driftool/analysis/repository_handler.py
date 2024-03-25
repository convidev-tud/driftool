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
from pathlib import Path
from datetime import datetime, timezone

import uuid
import subprocess
import os, os.path
import re
import stat

from driftool.data.pairwise_distance import PairwiseDistance
from driftool.analysis.directory import purge_blacklist, keep_whitelist

class RepositoryHandler:

    _input_dir: str
    _fetch_updates: bool 
    _reference_tmp_path : str | None
    _working_tmp_path: str | None
    _branch_ignores: list[str]
    _file_ignores: list[str]
    _file_whitelist: list[str]
    _timeout_days: int
    
    branches: list[str]
    head: str | None = None


    def __init__(self, input_dir, fetch_updates: bool, ignore_files: list[str], 
                 whitelist_files: list[str], ignore_branches: list[str], timeout_days: int):
        '''
        Specify all required arguments to setup the repository analysis.
        In multithreading usecases only:
        If a RepositoryHandler object only serves as a pseudo object and the actual reference tmp is already
        created by a real RepositoryHandler, all values can set to dummy values as the arguments are not needed.
        In this case:
        You must use set_bypass_arguments(...) with a valid tmp path once.
        You can afterward only use merge_and_count_conflicts(...) and NO other method of this class!
        '''
        self._input_dir = input_dir
        self._fetch_updates = fetch_updates
        self._branch_ignores = ignore_branches
        self._file_ignores = ignore_files
        self._file_whitelist = whitelist_files
        self._timeout_days = timeout_days
        self.branches = list()
        self.log = list()


    def create_reference_tmp(self):
        self._reference_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._input_dir, self._reference_tmp_path)


    def set_bypass_arguments(self, reference_path):
        '''
        Set paramters without materializing them regularly.
        Used for multihreading usecases.
        '''
        self._reference_tmp_path = reference_path


    def commit_file_selectors(self):

        if len(self._file_whitelist) > 0 or len(self._file_ignores) > 0:

            if len(self._file_whitelist) > 0:
                # delete everything EXCEPT the whitelist
                keep_whitelist(self._file_whitelist, self._reference_tmp_path + "/")
            if len(self._file_ignores) > 0:
                # delete everything from the blacklist
                purge_blacklist(self._file_ignores, self._reference_tmp_path + "/")
                
            out1 = subprocess.run(["git", "add", "--all"], capture_output=True, cwd=self._reference_tmp_path).stdout
            out2 = subprocess.run(["git", "commit", "-m", '"close setup (driftool)"'], capture_output=True, cwd=self._reference_tmp_path).stdout


    def clear_reference_tmp(self):
        try:
            os.access(self._reference_tmp_path, stat.S_IWUSR)
            rmtree(self._reference_tmp_path)
            self._reference_tmp_path = None
            print("TMP files cleaned")
        except:
            print("DELETE TMP/ FILE MANUALLY!")


    def create_working_tmp(self):
        self._working_tmp_path = "./tmp/" + str(uuid.uuid4())
        copytree(self._reference_tmp_path, self._working_tmp_path)
        out1 = subprocess.run(["git", "config", "user.name", '"driftool"'], capture_output=True, cwd=self._reference_tmp_path).stdout
        out2 = subprocess.run(["git", "config", "user.email", '"analysis@driftool.io"'], capture_output=True, cwd=self._reference_tmp_path).stdout


    def reset_working_tmp(self):
        cancel_merge = subprocess.run(["git", "merge", "--abort"], capture_output=True, cwd=self._working_tmp_path)
        if self.head is not None:
            reset = subprocess.run(["git", "reset", "--hard", self.head], capture_output=True, cwd=self._working_tmp_path)
        stash_clutter = subprocess.run(["git", "clean", "-f", "-d"], capture_output=True, cwd=self._working_tmp_path)


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

        # create regexes to find ignored branches
        excludes = list()
        for rule in self._branch_ignores:
            excludes.append(re.compile(rule))

        last_commits = self.get_branch_activity()
        print(last_commits)

        # Check if a branch is ignored because of the regex or the commit-date timeout
        self.branches = list()

        # checkout every analyzed branch locally
        for branch in all_branches:
            
            print(branch)
            
            ignore = False
            for expr in excludes:
                match = expr.search(branch)
                if match is not None:
                    ignore = True
                    break
                
            # FIXME this seems not to work right now.    
            
            if branch in last_commits:
                if self._timeout_days > 0 and last_commits[branch] > self._timeout_days:
                    ignore = True
            else:
                self.log.append("Branch " + branch + " not found in last_commits")
                
            
            # Do not analyse the branch (do also not checkout) if it is ignored to save processing time
            if ignore:
                print("---> IGNORE")
                continue
            
            print("---> KEEP")
            self.branches.append(branch)
            subprocess.run(["git", "checkout", branch], capture_output=True, cwd=path)
            subprocess.run(["git", "clean", "-f", "-d"], capture_output=True, cwd=path)
            
            if self._fetch_updates:
                pull = subprocess.run(["git", "pull", "origin", branch], capture_output=True, cwd=path).stdout

            self.commit_file_selectors()
        
        self.branches.sort()
        
        return self.branches
    
    
    def get_branch_activity(self) -> dict:
        # Get the last commit timestamps for each branch
        # git branch -l --format="%(committerdate:iso8601)~%(refname:short)" | grep -v HEAD
        # EXAMPLES:
        # 2024-03-02 20:52:47 +0100~12-dockerization
        # 2024-02-26 17:41:57 +0100~main
        # 2024-02-26 17:41:57 +0100~origin
        # 2021-04-15 16:10:35 +0200~origin/issue/2713/text-editor-unlink
        # 2021-05-24 16:48:03 +0200~origin/issue/4405/add-menu-link-avatar
        datestrings = subprocess.run('git branch -a --format="%(committerdate:iso8601)~%(refname:short)" | grep -v HEAD', 
                                     capture_output=True, shell=True, cwd=self._reference_tmp_path).stdout.decode("utf-8").split("\n")
        last_commits: dict = {}
        for datestring in datestrings:
            if not datestring:
                continue
            split = datestring.split("~")
            commit_date = datetime.fromisoformat(split[0])
            branch: str = split[1].replace("origin/", "")
            today = datetime.now(timezone.utc)
            last_commits[branch] = (today - commit_date).days
            self.log.append("RAW: " + datestring)
            self.log.append("Branch " + branch + " last commit: " + str(commit_date))
        return last_commits
    

    def merge_and_count_conflicts(self, base_branch: str, incoming_branch: str) -> PairwiseDistance:
        
        distance = PairwiseDistance()

        subprocess.run(["git", "checkout", base_branch], capture_output=True, cwd=self._working_tmp_path)
        
        m = re.compile("(?<=commit\\s)(.*?)(?=\\\n)")
        commit_log = subprocess.run(["git", "log", "-n 1"], capture_output=True, cwd=self._working_tmp_path).stdout
        commit_hash = m.search(commit_log.decode("utf-8")).group()
        self.head = commit_hash

        stdout_merge = subprocess.run(["git", "merge", incoming_branch], capture_output=True, cwd=self._working_tmp_path).stdout

        stdout_lines = map(lambda t: str(t), stdout_merge.splitlines())
        conflict_lines = list(filter(lambda s: ("Merge conflict in" in s), stdout_lines))
        conflict_files = list(map(lambda u: u.split("Merge conflict in ")[1], conflict_lines))
        conflict_files = list(map(lambda u: u[:len(u)-1], conflict_files))

        if len(conflict_files) > 0:

            #print(">> MERGE FROM " + incoming_branch + " INTO " + base_branch, end='\r')
            sum_of_conflicts = 0

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
                        conflict_start_line = line_index

                    if ">>>>>>>" in line and inside_conflict:
                        # merge conflict end
                        inside_conflict = False
                        sum_of_conflicts += (line_index - conflict_start_line)

                    line_index += 1

                #print("conflicting lines: " + str(sum_of_conflicts))
                distance.conflicting_lines = sum_of_conflicts
        
        return distance
