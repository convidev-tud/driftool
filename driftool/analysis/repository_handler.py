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

from shutil import rmtree
from datetime import datetime, timezone

import uuid
import subprocess
import os, os.path
import re
import pytz
import stat

from driftool.analysis.directory import purge_blacklist, keep_whitelist, count_files, copy_dir

class RepositoryHandler:

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
        self.branches : list[str]= list()
        
        self.log: list[str] = list()
        self.log.append("##########REPOSITORY_HANDLER##########")


    def create_reference_tmp(self):
        self._reference_tmp_path = "./tmp/" + str(uuid.uuid4())
        copy_dir("./", self._input_dir + "/" , self._reference_tmp_path)
        out1 = subprocess.run(["git", "config", "user.name", '"driftool"'], capture_output=True, cwd=self._reference_tmp_path).stdout
        out2 = subprocess.run(["git", "config", "user.email", '"analysis@driftool.io"'], capture_output=True, cwd=self._reference_tmp_path).stdout



    def set_bypass_arguments(self, reference_path):
        '''
        Set paramters without materializing them regularly.
        Used for multihreading usecases.
        '''
        self._reference_tmp_path = reference_path
        self.log.append("Bypassing arguments for multithreading")


    def clear_reference_tmp(self):
        os.access(self._reference_tmp_path, stat.S_IWUSR)
        rmtree(self._reference_tmp_path)
        self._reference_tmp_path = None


    def create_working_tmp(self):
        self.log.append("Creating working tmp")
        self._working_tmp_path = "./tmp/" + str(uuid.uuid4())
        copy_dir("./", self._reference_tmp_path + "/", self._working_tmp_path)

        git_user = subprocess.run(["git", "config", "user.name", '"driftool"'], capture_output=True, cwd=self._working_tmp_path)
        if git_user.returncode != 0:
            self.log.append(str(git_user.stderr.decode("utf-8")))
            raise Exception("Failed to create git user")
        
        git_mail = subprocess.run(["git", "config", "user.email", '"analysis@driftool.io"'], capture_output=True, cwd=self._working_tmp_path)
        if git_mail.returncode != 0:
            self.log.append(str(git_mail.stderr.decode("utf-8")))
            raise Exception("Failed to set git mail")
        
        self.log.append("FILE COUNT IN REFERENECE: " + str(count_files(self._reference_tmp_path)))
        self.log.append("FILE COUNT IN WORKING: " + str(count_files(self._working_tmp_path)))


    def clear_working_tmp(self):
        self.log.append("Clearing working tmp")
        os.access(self._working_tmp_path, stat.S_IWUSR)
        rmtree(self._working_tmp_path)
        self._working_tmp_path = None


    def materialize_all_branches_in_reference(self) -> list[str]:
        
        self.log.append(">>> Start materialize_all_branches_in_reference")

        path = self._reference_tmp_path
        os.access(path, stat.S_IWUSR)

        remote_branches_raw = subprocess.run(["git", "branch", "--all"], capture_output=True, cwd=path).stdout.decode("utf-8")
        self.log.append("all materilized branches:")
        self.log.append(remote_branches_raw)

        all_branches: list[str] = list()

        for line in remote_branches_raw.split("\n"):
            line = line.replace("remotes/origin/", "").replace("*", "").replace(" ", "")
            if not line in all_branches and not "HEAD->" in line and not line.isspace() and line != "":
                all_branches.append(line)
                self.log.append("added branch of interest: " + line)

        # create regexes to find ignored branches
        excludes = list()
        for rule in self._branch_ignores:
            excludes.append(re.compile(rule))

        last_commits = self.get_branch_activity()
        self.branches = list()
        all_branches.sort()

        # Check if a branch is ignored because of the regex or the commit-date timeout
        # checkout every analyzed branch locally
        for branch in all_branches:
            
            ignore = False

            for expr in excludes:
                match = expr.search(branch)
                if match is not None:
                    ignore = True
                    break   
            
            if branch in last_commits:
                if self._timeout_days > 0 and last_commits[branch] > self._timeout_days:
                    ignore = True
            else:
                self.log.append("PARSING PROBLEM: Branch " + branch + " not found in last_commits")
                ignore = True
                
            # Do not analyse the branch (do also not checkout) if it is ignored to save processing time
            if ignore:
                self.log.append("IGNORE branch " + branch)
                #TODO delete the branch to improve performance
                continue
            
            self.branches.append(branch)

            res_status = subprocess.run(["git", "status"], capture_output=True, cwd=path)
            self.log.append("STATUS: " + str(res_status.stdout.decode("utf-8")))

            #self.reset_hard(path, self.log)
            #self.clean_f_d_x(path, self.log)

            res_checkout = subprocess.run(["git", "checkout", branch], capture_output=True, cwd=path)

            self.reset_hard(path, self.log)
            self.clean_f_d_x(path, self.log)

            if res_checkout.returncode != 0:
                self.log.append(str(res_checkout.stderr.decode("utf-8")))
                raise Exception("Failed to materialize branches of interest")
            
            self.log.append("FILE COUNT IN: " + branch + " = " + str(count_files(path)))

            #if self._fetch_updates:
            #    pull = subprocess.run(["git", "pull", "origin", branch], capture_output=True, cwd=path).stdout

            self.log.append("Set file selectors for: " + branch)
            
            self.commit_file_selectors()
            
            self.reset_hard(path, self.log)
            self.clean_f_d_x(path, self.log)


        self.branches.sort()
        self.log.append("Sorted branch list: " + str(self.branches))
        self.log.append("<<< End materialize_all_branches_in_reference")
        
        return self.branches
    
    
    def clean_f_d_x(self, path: str, log: list[str] | None):
        if log is not None: 
            log.append("Clean branch")
        res_clean = subprocess.run(["git", "clean", "-f", "-d", "-x"], capture_output=True, cwd=path)
        if log is not None:
            self.log.append(str(res_clean.stdout.decode("utf-8")))
            if res_clean.returncode != 0:
                self.log.append(str(res_clean.stderr.decode("utf-8")))
        if res_clean.returncode != 0:
            raise Exception("Failed to materialize branches of interest")
        

    def reset_hard(self, path: str, log: list[str] | None):
        reset = subprocess.run(["git", "reset", "--hard"], capture_output=True, cwd=path)
        if log is not None:
            log.append(str(reset.stdout.decode("utf-8")))
        if reset.returncode != 0:
            if log is not None:
                self.log.append(str(reset.stderr.decode("utf-8")))
            raise Exception("Failed to reset base branch")


    def commit_file_selectors(self):

        self.log.append(">>> Commit file selectors")
        if len(self._file_whitelist) > 0 or len(self._file_ignores) > 0:

            # delete everything EXCEPT the whitelist
            if len(self._file_whitelist) > 0:
                self.log.append("WHITELIST: " + str(self._file_whitelist))
                keep_whitelist(self._file_whitelist, self._reference_tmp_path + "/", True, self.log)

            # delete everything from the blacklist
            if len(self._file_ignores) > 0:
                self.log.append("BLACKLIST: " + str(self._file_ignores))
                purge_blacklist(self._file_ignores, self._reference_tmp_path + "/", True, self.log)
                
            out1 = subprocess.run(["git", "add", "--all"], capture_output=True, cwd=self._reference_tmp_path)
            
            if out1.returncode != 0:
                self.log.append(str(out1.stderr.decode("utf-8")))
                raise Exception("Failed to add selected files")
            
            out2 = subprocess.run(["git", "commit", "-m", '"close setup (driftool)"'], capture_output=True, cwd=self._reference_tmp_path)
            if out2.returncode != 0:
                self.log.append(str(out2.stderr.decode("utf-8")))
                raise Exception("Failed to commit file selectors")


    def get_branch_activity(self) -> dict:
        self.log.append(">>> Start get_branch_activity")
        # Get the last commit timestamps for each branch
        # git branch -l --format="%(committerdate:iso8601)~%(refname:short)" | grep -v HEAD
        # EXAMPLES:
        # 2024-03-02 20:52:47 +0100~12-dockerization
        # 2024-02-26 17:41:57 +0100~main
        # 2024-02-26 17:41:57 +0100~origin
        # 2021-04-15 16:10:35 +0200~origin/issue/2713/text-editor-unlink
        # 2021-05-24 16:48:03 +0200~origin/issue/4405/add-menu-link-avatar
        
        datestrings = subprocess.run('git branch -a --format="%(committerdate:short)~%(refname:short)" | grep -v HEAD', 
                                     capture_output=True, shell=True, cwd=self._reference_tmp_path).stdout.decode("utf-8").split("\n")
        last_commits: dict = {}
        for datestring in datestrings:
            if not datestring:
                continue
            split = datestring.split("~")
            commit_date = datetime.fromisoformat(split[0])
            commit_date = commit_date.replace(tzinfo=pytz.UTC).replace(hour=12, minute=0, second=0, microsecond=0)
            branch = split[1]
            if branch.startswith("origin/"):
                branch = branch.replace("origin/", "")
                
            # Setting the timeout time to always 12:00 avoids timout problems depending on the analysis time.
            # Consequently, all analysis runs of the same day will lead to the same results.
            today = datetime.now(timezone.utc).replace(hour=12, minute=0, second=0, microsecond=0)
            last_commits[branch] = (today - commit_date).days
            self.log.append("RAW: " + datestring)
            self.log.append("Branch " + branch + " last commit: " + str(commit_date))
            
        self.log.append("<<< End get_branch_activity")
        return last_commits
    

    def merge_and_count_conflicts(self, base_branch: str, incoming_branch: str) -> int:

        os.access(self._working_tmp_path, stat.S_IWUSR)

        self.log.append(">>> Start merge_and_count_conflicts")
        self.log.append("Merge from " + incoming_branch + " into " + base_branch)

        # ClEAN WHATEVER RBANCH WE ARE CURRENTLY IN

        self.reset_hard(self._working_tmp_path, self.log)
        self.clean_f_d_x(self._working_tmp_path, self.log)

        # CHECKOUT AND CLEAN INCOMING BRANCH

        checkout = subprocess.run(["git", "checkout", incoming_branch], capture_output=True, cwd=self._working_tmp_path)
        self.log.append("Checkout incoming branch " + incoming_branch)
        self.log.append(str(checkout.stdout.decode("utf-8")))

        if checkout.returncode != 0:
            self.log.append(str(checkout.stderr.decode("utf-8")))
            raise Exception("Failed to checkout base branch")

        self.reset_hard(self._working_tmp_path, self.log)
        self.clean_f_d_x(self._working_tmp_path, self.log)

        # CHECKOUT AND CLEAN BASE BRANCH

        checkout = subprocess.run(["git", "checkout", base_branch], capture_output=True, cwd=self._working_tmp_path)
        self.log.append("Checkout base branch " + base_branch)
        self.log.append(str(checkout.stdout.decode("utf-8")))

        if checkout.returncode != 0:
            self.log.append(str(checkout.stderr.decode("utf-8")))
            raise Exception("Failed to checkout base branch")

        self.reset_hard(self._working_tmp_path, self.log)
        self.clean_f_d_x(self._working_tmp_path, self.log)

        # DO MERGE
        
        self.log.append("FILE COUNT IN BASE: " + str(count_files(self._working_tmp_path)))

        
        self.log.append("ATTEMPT MERGE")
        self.log.append("Base branch: " + base_branch)
        self.log.append("Incoming branch: " + incoming_branch)

        merge_res = subprocess.run(["git", "merge", incoming_branch], capture_output=True, cwd=self._working_tmp_path)
        
        stdout_merge = merge_res.stdout
        self.log.append("#### STDOUT ####")
        self.log.append(str(stdout_merge.decode("utf-8")))

        stderr_merge = merge_res.stderr
        self.log.append("#### STDERR ####")
        self.log.append(str(stderr_merge.decode("utf-8")))

        self.log.append("--------lines")
        stdout_lines: list[str] = list()

        for line in stdout_merge.splitlines():
            stdout_lines.append(line.decode("utf-8"))
            self.log.append(line.decode("utf-8") + "\n")
        
        self.log.append("--------conflict lines")
        conflict_lines: list[str] = list()
        
        for line in stdout_lines:
            if "Merge conflict in" in line:
                conflict_lines.append(line)
                self.log.append(line + "\n")
        
        self.log.append("--------conflict files")
        
        conflict_files: list[str] = list()
        for line in conflict_lines:
            conflict_files.append(line.split("Merge conflict in ")[1].strip())
            self.log.append(line.split("Merge conflict in ")[1].strip() + "\n")
            
        self.log.append("--------")
        
        distance = 0

        if len(conflict_files) > 0:

            for file in conflict_files:

                opened_file = None
                conflicting_file: list[str] = list()

                try:
                    opened_file = open(self._working_tmp_path + "/" + file, "r", encoding='utf-8', errors='strict')
                    conflicting_file = opened_file.readlines()
                except Exception as e:
                    self.log.append("Error: cannot open conflicting file: " + file)
                    self.log.append(str(e))
                    self.log.append("--> Proceed without action")
                    opened_file.close()
                    conflicting_file = list()
                    continue

                inside_conflict = False
                conflict_start_line = 0
                line_index = 0

                for line in conflicting_file:

                    if line.strip().startswith("<<<<<<<") and not inside_conflict:
                        # merge conflict start
                        inside_conflict = True
                        conflict_start_line = line_index

                    if line.strip().startswith(">>>>>>>") and inside_conflict:
                        # merge conflict end
                        inside_conflict = False
                        distance += (line_index - conflict_start_line)

                    line_index += 1

                opened_file.close()
        
        self.log.append("<<< End merge_and_count_conflicts")

        return distance
