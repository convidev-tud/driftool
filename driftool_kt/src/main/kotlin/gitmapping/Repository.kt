/**
 * Copyright 2024 Karl Kegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.driftool.gitmapping

import io.driftool.Log
import io.driftool.shell.Shell
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries

class Repository(val location: String) {

    private val allBranches: MutableList<String> = mutableListOf()
    private val branchesOfInterest: MutableList<String> = mutableListOf()
    private var currentBranch: String? = null
    var defaultBranch: String? = null

    fun findAllBranches(): List<String> {
        val listBranchResult = Shell.exec(arrayOf("git", "branch", "--all"), location)

        if (! listBranchResult.isSuccessful()){
            throw RuntimeException("Could not list branches: git branch --all failed in given location")
        }
        val stdout = listBranchResult.output
        val allBranches: MutableList<String> = mutableListOf()

        for (line in stdout.split("\n")){
            val cleanedLine = line
                .replace("remotes/origin/", "")
                .replace("*", "")
                .replace(" ", "")
            if(cleanedLine.isNotEmpty() && !cleanedLine.contains("HEAD->") && cleanedLine.isNotBlank() && !allBranches.contains(cleanedLine)){
                allBranches.add(cleanedLine)
                Log.append("added branch : $cleanedLine")
            }
        }
        this.allBranches.clear()
        this.allBranches.addAll(allBranches)
        return allBranches
    }

    /**
     * Returns a list of branches that are not ignored by the repository.
     * This applies the list of ignored branches.
     * This also applies the timeout days.
     * The branches of interest are returned and stored in the [branchesOfInterest] list.
     * @param timeoutDays The number of days without activity after that a branch is considered to be out of interest.
     * @param ignoreBranchesPatterns A list of regular expressions that define branches that should be ignored.
     * @return A list of branches that are not ignored
     */
    fun findBranchesOfInterest(timeoutDays: Int, ignoreBranchesPatterns: List<String>): List<String> {
        Log.append(">>> Start getBranchesOfInterest")

        val lastCommitDatePerBranch: Map<String, Instant> = findModificationDates()
        val todayAtNoonUTC = Instant.now().atZone(ZoneOffset.UTC).withHour(12).withMinute(0).withSecond(0).withNano(0).toInstant()
        val todayAtNoonUTCMinusTimeout = todayAtNoonUTC.minusSeconds(timeoutDays.toLong() * 24 * 60 * 60)

        //Check if a branch is ignored because of the regex or the commit-date timeout
        //checkout every analyzed branch locally

        for (branch in allBranches){
            var ignore = false
            for (expr in ignoreBranchesPatterns){
                val match = expr.toRegex().find(branch)
                if (match != null){
                    ignore = true
                    break
                }
            }
            if (branch in lastCommitDatePerBranch){
                if (timeoutDays > 0 && lastCommitDatePerBranch[branch]!!.isBefore(todayAtNoonUTCMinusTimeout) ){
                    ignore = true
                }
            } else {
                Log.append("PARSING PROBLEM: Branch $branch not found in last_commits")
                ignore = true
            }
            if (!ignore){
                Log.append("Branch $branch is not ignored")
                branchesOfInterest.add(branch)
            }else{
                Log.append("Branch $branch is ignored")
            }
        }

        return branchesOfInterest
    }

    fun overrideBranchesOfInterest(branches: List<String>){
        branchesOfInterest.clear()
        branchesOfInterest.addAll(branches)
    }

    fun getBranchesOfInterest(): List<String> {
        return branchesOfInterest
    }

    fun getCurrentBranch(): String {
        return currentBranch ?: throw RuntimeException("Current branch is not set")
    }

    fun getAllBranches(): List<String> {
        return allBranches
    }

    fun findModificationDates(): Map<String, Instant> {
        /*
        Get the last commit timestamps for each branch:
        2024-11-20~main
        2024-11-20~origin
        2024-11-20~origin/feature/a
        2024-11-20~origin/feature/b
        2024-11-20~origin/feature/c
        2024-11-20~origin/main
        */
        val lastCommitDates: MutableMap<String, Instant> = mutableMapOf()
        val gitDateStringsShellResult = Shell.execComplexCommand("git branch -a --format=\"%(committerdate:short)~%(refname:short)\" | grep -v HEAD", location)

        if(!gitDateStringsShellResult.isSuccessful()){
            throw RuntimeException("Could not get last commit timestamps for each branch")
        }
        val gitDateStrings = gitDateStringsShellResult.output.split("\n")

        for(dateString in gitDateStrings){
            if(! dateString.contains("~")){
                continue
            }
            val split = dateString.split("~")
            // Setting the timeout time to always 12:00 avoids timout problems depending on the analysis time.
            // Consequently, all analysis runs of the same day will lead to the same results.
            val commitDate: Instant = LocalDate.parse(split[0]).atStartOfDay(ZoneOffset.UTC).withHour(12).withMinute(0).withSecond(0).withNano(0).toInstant()

            var branch = split[1]
            if(branch.startsWith("origin/")){
                branch = branch.replace("origin/", "")
            }
            lastCommitDates.put(branch, commitDate)
            Log.append("RAW: $dateString")
            Log.append("Branch $branch last commit: $commitDate")
        }

        return lastCommitDates
    }

    fun applyWhiteList(whiteList: List<String>, rootLocation: String? = null) {
        Log.append("Applying whitelist to branch $currentBranch in location $location")
        val workingLocation = rootLocation ?: location
        applyPathList(whiteList, workingLocation, true)
    }

    fun applyBlackList(blackList: List<String>, rootLocation: String? = null) {
        Log.append("Applying blacklist to branch $currentBranch in location $location")
        val workingLocation = rootLocation ?: location
        applyPathList(blackList, workingLocation, false)
    }

    /**
     * FIXME: TO BE TESTED INTENSIVELY
     */
    private fun applyPathList(list: List<String>, rootLocation: String, keepMatches: Boolean){
        val gitPattern = Regex("\\.git")
        val patterns = list.map { it.toRegex() }
        val locationSuffix = rootLocation.removePrefix(location)
        Log.append("Applying list to locationSuffix: $locationSuffix")
        val allFilesInLocation: List<Path> = Path(rootLocation).listDirectoryEntries()

        for(elem in allFilesInLocation){
            Log.append("Checking file: ${elem.fileName} /// $elem")
            if(gitPattern.find(elem.toString()) != null){
                //Skip everything related to the git repository itself (.git/.gitignore/.gitkeep/...
                Log.append("--skip (git related)")
                continue
            }
            if(elem.isSymbolicLink()){
                Log.append("Symbolic link found: ${elem.fileName} -> deleting")
                delete(elem)
                Log.append("--delete")
                continue
            }
            if(elem.isDirectory()){
                Log.append("--traverse")
                if(keepMatches){
                    applyPathList(list, elem.toString(), true)
                }else{
                    applyPathList(list, elem.toString(), false)
                }
                continue
            }

            val elemSuffix = elem.toString().removePrefix(location)
            Log.append("--check file:  + $elemSuffix")
            val matchList: MutableList<Boolean> = mutableListOf()
            for(pattern in patterns){
                val isMatch = pattern.find(elemSuffix) != null
                matchList.add(isMatch)
                if(isMatch && !keepMatches){
                    break
                }
            }
            //case applying the blacklist and file is in the blacklist
            //if there is at least one match, then remove t
            if(matchList.contains(true) && !keepMatches) {
                Log.append("--delete")
                delete(elem)
                continue
            }
            //case applying the whitelist and file is not in the whitelist
            //delete the file of no match was found, i.e., no pattern to keep the file was specified
            if(matchList.count { it } == 0  && keepMatches){
                Log.append("--delete")
                delete(elem)
                continue
            }
        }
    }

    private fun delete(elem: Path){
        val result = Shell.rm(elem.toString())
        if(!result.isSuccessful()){
            Log.append("Could not delete file: ${elem.fileName}")
            Log.append(result.error)
        }
    }

    fun commitChanges(message: String) {
        Log.append("Committing changes to branch $currentBranch with message: $message")
        Log.append("Adding all files")

        val addResult = Shell.exec(arrayOf("git", "add", "--all"), location)
        if (! addResult.isSuccessful()){
            throw RuntimeException("Could not add all files to git")
        }
        val commitResult = Shell.exec(arrayOf("git", "commit", "-m", message), location)
        //if (! commitResult.isSuccessful()){
        //    throw RuntimeException("Could not commit changes to git")
        //}
    }

    fun initializeCurrentBranch() {
        Log.append("Initializing current branch")
        val branchShellResult = Shell.exec(arrayOf("git", "branch", "--show-current"), location)
        if (! branchShellResult.isSuccessful()){
            throw RuntimeException("Could not get current branch")
        }
        currentBranch = branchShellResult.output.trim()
    }

    fun checkoutBranch(branch: String) {
        Log.append("Checking out from $currentBranch into $branch")
        val result = Shell.exec(arrayOf("git", "checkout", branch), location)
        //if (! result.isSuccessful()){
        //    throw RuntimeException("Could not checkout from $currentBranch into $branch")
        //}
        currentBranch = branch
    }

    fun resetHard() {
        Log.append("Resetting current branch $currentBranch")
        val result = Shell.exec(arrayOf("git", "reset", "--hard"), location)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not reset current branch $currentBranch")
        }
    }

    fun cleanFDX() {
        Log.append("Cleaning (-fdx) current branch $currentBranch")
        val result = Shell.exec(arrayOf("git", "clean", "-fdx"), location)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not clean current branch $currentBranch")
        }
    }

    fun sanitize() {
        Log.append("Sanitizing current branch $currentBranch")
        resetHard()
        cleanFDX()
    }

    fun mergeAndCountConflicts(branch: String): Int {
        /*

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
         */
        throw NotImplementedError()
    }

    fun deleteRepository() {
        Log.append("Deleting repository at $location")
        val result = Shell.rmrf(location, null)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not delete repository at $location")
        }
    }

    companion object {

        /**
         * Creates a Repository object by copying a given repository folder to a new location.
         * The repository object of the new location is returned.
         * The target location must be an empty directory that is either already registered by a [DirectoryHandler] or
         * must be registered afterward.
         * Both the path and the location must be absolute paths!
         * @param absoluteRepositoryPath The path to the repository folder that should be copied.
         * @param location The target location where the repository folder should be copied to.
         * @return The repository object of the new location.
         */
        fun cloneFromPath(absoluteRepositoryPath: String, location: String): Repository {
            Log.append("Cloning repository from path: $absoluteRepositoryPath")
            Log.append("Cloning repository to location: $location")
            val cpResult = Shell.cp(
                DirectoryHandler.ensureDirectoryPathEnding(absoluteRepositoryPath),
                DirectoryHandler.ensureNoDirectoryPathEnding(location), null)

            Log.append("Exciting Shell Command: " + cpResult.exitCode)
            Log.append("Shell Output: " + cpResult.output)
            Log.append("Shell Error: " + cpResult.error)

            if (! cpResult.isSuccessful()){
                Log.append(cpResult.error)
                throw RuntimeException("Could not copy repository to new temporal location.")
            }
            return Repository(location)
        }

    }
}